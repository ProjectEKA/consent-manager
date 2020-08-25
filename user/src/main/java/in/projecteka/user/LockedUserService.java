package in.projecteka.user;

import in.projecteka.user.properties.LockedServiceProperties;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static in.projecteka.library.clients.model.ClientError.userBlocked;
import static java.time.LocalDateTime.now;
import static java.time.ZoneOffset.UTC;
import static reactor.core.publisher.Mono.defer;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.just;

@AllArgsConstructor
public class LockedUserService {
    private final LockedUsersRepository lockedUsersRepository;
    private final LockedServiceProperties lockedServiceProperties;

    public Mono<String> validateLogin(String cmId) {
        return lockedUsersRepository.getLockedUserFor(cmId)
                .filter(lockedUser -> lockedUser.getInvalidAttempts() >= lockedServiceProperties.getMaximumInvalidAttempts())
                .flatMap(lockedUser -> {
                    var isBlocked = isBeforeMinutes(lockedUser.getDateModified(), lockedServiceProperties.getCoolOfPeriod());
                    return isBlocked ? error(userBlocked()) : removeLockedUser(cmId);
                }).thenReturn(cmId);
    }

    public Mono<Void> removeLockedUser(String cmId) {
        return lockedUsersRepository.deleteUser(cmId);
    }

    public Mono<Integer> createOrUpdateLockedUser(String cmId) {
        return lockedUsersRepository.getLockedUserFor(cmId)
                .flatMap(lockedUser -> {
                    var remainingTries = lockedServiceProperties.getMaximumInvalidAttempts() - lockedUser.getInvalidAttempts();
                    return isBeforeMinutes(lockedUser.getDateCreated(), lockedServiceProperties.getCoolOfPeriod())
                           ? just(remainingTries - 1)
                           : removeLockedUser(cmId).thenReturn(lockedServiceProperties.getMaximumInvalidAttempts() - 1);
                })
                .flatMap(remainingTries -> lockedUsersRepository.upsert(cmId)
                        .thenReturn(remainingTries))
                .switchIfEmpty(defer(() -> lockedUsersRepository.upsert(cmId)
                        .thenReturn(lockedServiceProperties.getMaximumInvalidAttempts() - 1)));
    }

    private boolean isBeforeMinutes(LocalDateTime timeToCheck, int minutesToCheckWith) {
        return timeToCheck.plusMinutes(minutesToCheckWith).isAfter(now(UTC));
    }
}
