package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@AllArgsConstructor
public class LockedUserService {
    private final LockedUsersRepository lockedUsersRepository;
    private final LockedServiceProperties lockedServiceProperties;

    public Mono<Void> validateLogin(String cmId) {
        return lockedUsersRepository.getLockedUserFor(cmId)
                .filter(lockedUser -> lockedUser.getInvalidAttempts() >= lockedServiceProperties.getMaximumInvalidAttempts())
                .flatMap(lockedUser -> {
                    var isBlocked = isBeforeMinutes(lockedUser.getDateModified(), lockedServiceProperties.getCoolOfPeriod());
                    return isBlocked ? Mono.error(ClientError.userBlocked()) : removeLockedUser(cmId);
                });
    }

    public Mono<Void> removeLockedUser(String cmId) {
        return lockedUsersRepository.deleteUser(cmId);
    }

    public Mono<Void> createOrUpdateLockedUser(String cmId) {
        return lockedUsersRepository.getLockedUserFor(cmId)
                .flatMap(lockedUser ->
                        isBeforeMinutes(lockedUser.getDateCreated(), lockedServiceProperties.getCoolOfPeriod())
                                ? Mono.empty() : removeLockedUser(cmId))
                .then(lockedUsersRepository.upsert(cmId));
    }

    public Mono<Integer> tempreateOrUpdateLockedUser(String cmId) {
        return lockedUsersRepository.getLockedUserFor(cmId)
                .flatMap(lockedUser -> {
                    System.out.println("inside ------");
                    var remainingTries = lockedServiceProperties.getMaximumInvalidAttempts() - lockedUser.getInvalidAttempts();
                    return isBeforeMinutes(lockedUser.getDateCreated(), lockedServiceProperties.getCoolOfPeriod())
                            ? Mono.just(remainingTries)
                            : removeLockedUser(cmId).thenReturn(lockedServiceProperties.getMaximumInvalidAttempts() -1);
                })
                .flatMap(remainingTries -> lockedUsersRepository.upsert(cmId).thenReturn(remainingTries));
    }

    private boolean isBeforeMinutes(LocalDateTime timeToCheck, int minutesToCheckWith) {
        return timeToCheck
                .plusMinutes(minutesToCheckWith)
                .isAfter(LocalDateTime.now(ZoneOffset.UTC));
    }
}
