package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.OtpServiceClient;
import in.projecteka.consentmanager.clients.model.OtpCommunicationData;
import in.projecteka.consentmanager.clients.model.OtpRequest;
import in.projecteka.consentmanager.clients.model.Session;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import in.projecteka.consentmanager.user.model.LogoutRequest;
import in.projecteka.consentmanager.user.model.OtpPermitRequest;
import in.projecteka.consentmanager.user.model.OtpVerificationRequest;
import in.projecteka.consentmanager.user.model.SessionRequest;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static in.projecteka.consentmanager.common.Constants.BLACKLIST;
import static in.projecteka.consentmanager.common.Constants.BLACKLIST_FORMAT;

@AllArgsConstructor
public class SessionService {

    private final TokenService tokenService;
    private final CacheAdapter<String, String> blacklistedTokens;
    private final Logger logger = LoggerFactory.getLogger(SessionService.class);
    private final UserRepository userRepository;
    private final OtpServiceClient otpServiceClient;

    public Mono<Session> forNew(SessionRequest request) {
        if (StringUtils.isEmpty(request.getUsername()) || StringUtils.isEmpty(request.getPassword()))
            return Mono.error(ClientError.unAuthorizedRequest("Username or password is incorrect"));
        return tokenService.tokenForUser(request.getUsername(), request.getPassword())
                .doOnError(error -> logger.error(error.getMessage(), error))
                .onErrorResume(error -> Mono.error(ClientError.unAuthorizedRequest("Username or password is incorrect")));
    }

    public Mono<Void> logout(String accessToken, LogoutRequest logoutRequest) {
        return blacklistedTokens.put(String.format(BLACKLIST_FORMAT, BLACKLIST, accessToken), "")
                .then(tokenService.revoke(logoutRequest.getRefreshToken()));
    }

    public Mono<String> sendOtp(OtpVerificationRequest otpVerificationRequest) {
        String sessionId = UUID.randomUUID().toString();
        return userRepository.userWith(otpVerificationRequest.getUsername())
                .switchIfEmpty(Mono.error(ClientError.userNotFound()))
                .map(user -> new OtpCommunicationData("mobile",user.getPhone()))
                .map(otpCommunicationData -> new OtpRequest(sessionId,otpCommunicationData))
                .flatMap(otpServiceClient::send)
                .thenReturn(sessionId);
    }

    public Mono<Session> validateOtp(OtpPermitRequest otpPermitRequest) {
        return tokenService
                .tokenForOtpUser(otpPermitRequest.getUsername(),otpPermitRequest.getSessionId(),otpPermitRequest.getOtp());
    }
}
