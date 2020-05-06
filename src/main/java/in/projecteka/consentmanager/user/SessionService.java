package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.model.Session;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import in.projecteka.consentmanager.user.exception.InvalidUserNameException;
import in.projecteka.consentmanager.user.model.LockedUser;
import in.projecteka.consentmanager.user.model.LogoutRequest;
import in.projecteka.consentmanager.user.model.SessionRequest;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.Date;

import static in.projecteka.consentmanager.common.Constants.BLACKLIST;
import static in.projecteka.consentmanager.common.Constants.BLACKLIST_FORMAT;

@AllArgsConstructor
public class SessionService {

    private final TokenService tokenService;
    private final CacheAdapter<String, String> blacklistedTokens;
    private final Logger logger = LoggerFactory.getLogger(SessionService.class);
    private final LockedUsersRepository lockedUsersRepository;

    public Mono<Session> forNew(SessionRequest request) {
        if (StringUtils.isEmpty(request.getUsername()) || StringUtils.isEmpty(request.getPassword()))
            return Mono.error(ClientError.unAuthorizedRequest());
        return tokenService.tokenForUser(request.getUsername(), request.getPassword())
                .doOnError(error -> logger.error(error.getMessage(), error))
                .onErrorResume(InvalidUserNameException.class, error -> Mono.error(ClientError.unAuthorizedRequest1()))
                .onErrorResume(error -> lockedUsersRepository.getLockedUserFor(request.getUsername())
                        .switchIfEmpty(
                                lockedUsersRepository.insert(
                                        new LockedUser(1, request.getUsername(), false, null))
                                        .then(Mono.error(ClientError.unAuthorizedRequest())))
                        .flatMap(optionalLockedUser -> {
                            var isLocked = false;
                            var blockedTime = "";
                            var clientError = ClientError.unAuthorizedRequest();
                            if (optionalLockedUser.getInvalidAttempts() >= 5) {
                                isLocked = true;
                                blockedTime = new Date().toString();
                                clientError = ClientError.userBlocked();
                            }
                            return lockedUsersRepository
                                    .updateUser(isLocked, blockedTime,
                                            optionalLockedUser.getPatientId(), optionalLockedUser.getInvalidAttempts() + 1)
                                    .then(Mono.error(clientError));
                        }));
    }

    public Mono<Void> logout(String accessToken, LogoutRequest logoutRequest) {
        return blacklistedTokens.put(String.format(BLACKLIST_FORMAT, BLACKLIST, accessToken), "")
                .then(tokenService.revoke(logoutRequest.getRefreshToken()));
    }
}
