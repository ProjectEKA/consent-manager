package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.OtpServiceClient;
import in.projecteka.consentmanager.clients.model.Meta;
import in.projecteka.consentmanager.clients.model.OtpCommunicationData;
import in.projecteka.consentmanager.clients.model.OtpRequest;
import in.projecteka.consentmanager.clients.model.Session;
import in.projecteka.consentmanager.clients.properties.OtpServiceProperties;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import in.projecteka.consentmanager.user.exception.InvalidUserNameException;
import in.projecteka.consentmanager.user.model.LockedUser;
import in.projecteka.consentmanager.user.model.LogoutRequest;
import in.projecteka.consentmanager.user.model.OtpPermitRequest;
import in.projecteka.consentmanager.user.model.OtpAttempt;
import in.projecteka.consentmanager.user.model.OtpVerificationRequest;
import in.projecteka.consentmanager.user.model.OtpVerificationResponse;
import in.projecteka.consentmanager.user.model.SessionRequest;
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

    private final TokenService tokenService;
    private final CacheAdapter<String, String> blacklistedTokens;
    private CacheAdapter<String, String> unverifiedSessions;
    private final Logger logger = LoggerFactory.getLogger(SessionService.class);

    private final LockedUserService lockedUserService;

    private final UserRepository userRepository;
    private final OtpServiceClient otpServiceClient;
    private final OtpServiceProperties otpServiceProperties;
    private final OtpAttemptService otpAttemptService;


    public Mono<Session> forNew(SessionRequest request) {
        if (StringUtils.isEmpty(request.getUsername()) || StringUtils.isEmpty(request.getPassword()))
            return Mono.error(ClientError.unAuthorizedRequest("Username or password is incorrect"));

        var newLockedUser = new LockedUser(0, request.getUsername(), false, "", "");
        Mono<LockedUser> lockedUser = lockedUserService.userFor(request.getUsername())
                .switchIfEmpty(
                        Mono.just(newLockedUser));

        return lockedUser
                .flatMap(user -> {
                    if (lockedUserService.isUserBlocked(user)) {
                        return Mono.error(ClientError.userBlocked());
                    } else {
                        return tokenService.tokenForUser(request.getUsername(), request.getPassword())
                                .doOnError(error -> logger.error(error.getMessage(), error))
                                .onErrorResume(InvalidUserNameException.class, error -> Mono.error(ClientError.invalidUserName()))
                                .onErrorResume(error -> lockedUserService.userFor(request.getUsername())
                                        .switchIfEmpty(
                                                lockedUserService.createUser(request.getUsername())
                                                        .then(Mono.error(ClientError.unAuthorizedRequest("Username or password is incorrect"))))
                                        .flatMap(optionalLockedUser -> lockedUserService.validateAndUpdate(optionalLockedUser).flatMap(Mono::error)));
                    }
                });
    }

    public Mono<Void> logout(String accessToken, LogoutRequest logoutRequest) {
        return blacklistedTokens.put(String.format(BLACKLIST_FORMAT, BLACKLIST, accessToken), "")
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
                .flatMap(userRepository::userWith)
                .flatMap(user -> {
                    OtpAttempt attempt = OtpAttempt.builder()
                            .action(OtpAttempt.Action.OTP_SUBMIT_LOGIN)
                            .cmId(otpPermitRequest.getUsername())
                            .sessionId(otpPermitRequest.getSessionId())
                            .identifierType("MOBILE")
                            .identifierValue(user.getPhone())
                            .build();
                    return otpAttemptService.validateOTPSubmission(attempt)
                            .then(tokenService
                                    .tokenForOtpUser(otpPermitRequest.getUsername(), otpPermitRequest.getSessionId(),
                                            otpPermitRequest.getOtp(),
                                            () -> otpAttemptService.createOtpAttemptFor(otpPermitRequest.getSessionId(),
                                                    otpPermitRequest.getUsername(), "MOBILE",
                                                    user.getPhone(), OtpAttempt.AttemptStatus.FAILURE,
                                                    OtpAttempt.Action.OTP_SUBMIT_LOGIN)))
                            .flatMap(session -> otpAttemptService.removeMatchingAttempts(attempt).then(Mono.just(session)));
                });
    }
}
