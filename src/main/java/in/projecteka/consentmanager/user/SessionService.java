package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.OtpServiceClient;
import in.projecteka.consentmanager.clients.model.ErrorCode;
import in.projecteka.consentmanager.clients.model.Meta;
import in.projecteka.consentmanager.clients.model.OtpCommunicationData;
import in.projecteka.consentmanager.clients.model.OtpRequest;
import in.projecteka.consentmanager.clients.model.Session;
import in.projecteka.consentmanager.clients.properties.OtpServiceProperties;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import in.projecteka.consentmanager.user.exception.InvalidPasswordException;
import in.projecteka.consentmanager.user.exception.InvalidRefreshTokenException;
import in.projecteka.consentmanager.user.exception.InvalidUserNameException;
import in.projecteka.consentmanager.user.model.LogoutRequest;
import in.projecteka.consentmanager.user.model.OtpAttempt;
import in.projecteka.consentmanager.user.model.OtpPermitRequest;
import in.projecteka.consentmanager.user.model.OtpVerificationRequest;
import in.projecteka.consentmanager.user.model.OtpVerificationResponse;
import in.projecteka.consentmanager.user.model.SessionRequest;
import in.projecteka.consentmanager.user.model.GrantType;
import io.vavr.Tuple2;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static in.projecteka.consentmanager.common.Constants.BLACKLIST;
import static in.projecteka.consentmanager.common.Constants.BLACKLIST_FORMAT;

@RequiredArgsConstructor
@AllArgsConstructor
public class SessionService {

    private final Logger logger = LoggerFactory.getLogger(SessionService.class);
    private final TokenService tokenService;
    private final CacheAdapter<String, String> blockListedTokens;
    private CacheAdapter<String, String> unverifiedSessions;
    private final LockedUserService lockedUserService;
    private final UserRepository userRepository;
    private final OtpServiceClient otpServiceClient;
    private final OtpServiceProperties otpServiceProperties;
    private final OtpAttemptService otpAttemptService;

    public Mono<Session> forNew(SessionRequest request) {
        var checkIsEmpty = credentialsNotEmpty(request);
        if(!checkIsEmpty._1){
            return Mono.error(checkIsEmpty._2);
        }
        return lockedUserService.validateLogin(request.getUsername())
                .then(request.getGrantType() == GrantType.PASSWORD
                        ? tokenService.tokenForUser(request.getUsername(), request.getPassword())
                        : tokenService.tokenForRefreshToken(request.getUsername(), request.getRefreshToken())
                        .flatMap(session -> lockedUserService.removeLockedUser(request.getUsername()).thenReturn(session))
                        .doOnError(error -> logger.error(error.getMessage(), error))
                        .onErrorResume(error ->
                        {
                            if (error instanceof InvalidUserNameException || error instanceof InvalidPasswordException) {
                                return lockedUserService.createOrUpdateLockedUser(request.getUsername())
                                        .then(Mono.error(ClientError.invalidUserNameOrPassword()));
                            }
                            else if (error instanceof InvalidRefreshTokenException) {
                                return lockedUserService.createOrUpdateLockedUser(request.getUsername())
                                        .then(Mono.error(ClientError.invalidRefreshToken()));
                            }
                            return Mono.error(error);
                        }));
    }
    private Tuple2<Boolean, ClientError> credentialsNotEmpty(SessionRequest request){
        if (request.getGrantType() == GrantType.PASSWORD
                && (StringUtils.isEmpty(request.getUsername()) || StringUtils.isEmpty(request.getPassword())))
            return new Tuple2(false, ClientError.invalidUserNameOrPassword());
        else if (request.getGrantType() == GrantType.REFRESH_TOKEN && (StringUtils.isEmpty(request.getRefreshToken())))
            return new Tuple2(false, ClientError.invalidRefreshToken());
        else if (StringUtils.isEmpty(request.getUsername()))
            return new Tuple2(false, ClientError.invalidUserNameOrPassword());
        return new Tuple2(true, null);
    }

    public Mono<Void> logout(String accessToken, LogoutRequest logoutRequest) {
        return blockListedTokens.put(String.format(BLACKLIST_FORMAT, BLACKLIST, accessToken), "")
                .then(tokenService.revoke(logoutRequest.getRefreshToken()));
    }

    public Mono<OtpVerificationResponse> sendOtp(OtpVerificationRequest otpVerificationRequest) {
        String sessionId = UUID.randomUUID().toString();
        return userRepository.userWith(otpVerificationRequest.getUsername())
                .switchIfEmpty(Mono.error(ClientError.userNotFound()))
                .map(user -> new OtpCommunicationData("mobile", user.getPhone()))
                .map(otpCommunicationData -> new OtpRequest(sessionId, otpCommunicationData))
                .flatMap(requestBody ->
                        otpAttemptService.validateOTPRequest(requestBody.getCommunication().getMode(), requestBody.getCommunication().getValue(), OtpAttempt.Action.OTP_REQUEST_LOGIN, otpVerificationRequest.getUsername())
                                .then(otpServiceClient.send(requestBody)
                                        .then(Mono.defer(() -> unverifiedSessions.put(sessionId, otpVerificationRequest.getUsername())))
                                        .thenReturn(requestBody.getCommunication().getValue())))
                .map(mobileNumber -> OtpVerificationResponse.builder()
                        .sessionId(sessionId)
                        .meta(Meta.builder()
                                .communicationExpiry(String.valueOf(otpServiceProperties.getExpiryInMinutes() * 60))
                                .communicationMedium("MOBILE")
                                .communicationHint(mask(mobileNumber))
                                .build())
                        .build());
    }

    private String mask(String mobileNumber) {
        return mobileNumber.replaceFirst("[0-9]{6}", "X".repeat(6));
    }

    public Mono<Session> validateOtp(OtpPermitRequest otpPermitRequest) {
        return unverifiedSessions.get(otpPermitRequest.getSessionId())
                .filter(username -> otpPermitRequest.getUsername().equals(username))
                .switchIfEmpty(Mono.error(ClientError.invalidSession(otpPermitRequest.getSessionId())))
                .flatMap(username -> lockedUserService.validateLogin(username)
                        .then(tokenService
                                .tokenForOtpUser(otpPermitRequest.getUsername(), otpPermitRequest.getSessionId(), otpPermitRequest.getOtp()))
                        .onErrorResume(ClientError.class, error -> {
                            if (error.getErrorCode() == ErrorCode.OTP_INVALID) {
                                return lockedUserService.createOrUpdateLockedUser(username).then(Mono.error(error));
                            }
                            return Mono.error(error);
                        })
                        .flatMap(session -> lockedUserService.removeLockedUser(username).thenReturn(session)));
    }
}
