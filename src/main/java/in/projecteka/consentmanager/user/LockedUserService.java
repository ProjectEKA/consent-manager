package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.user.model.LockedUser;
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
                    var isBlocked = isWithinTimeLimit(lockedUser, lockedServiceProperties.getCoolOfPeriod());
                    return isBlocked ? Mono.error(ClientError.userBlocked()) : removeLockedUser(cmId);
                });
    }

    public Mono<Void> removeLockedUser(String cmId) {
        return lockedUsersRepository.deleteUser(cmId);
    }

    public Mono<Void> createOrUpdateLockedUser(String cmId) {
        return lockedUsersRepository.getLockedUserFor(cmId)
                .flatMap(lockedUser ->
                        isWithinTimeLimit(lockedUser, lockedServiceProperties.getCoolOfPeriod()) ? Mono.empty() : removeLockedUser(cmId))
                .then(lockedUsersRepository.upsert(cmId));
    }

    private boolean isWithinTimeLimit(LockedUser user, int timeLimitInMin) {
        return user
                .getDateModified()
                .plusMinutes(timeLimitInMin)
                .isAfter(LocalDateTime.now(ZoneOffset.UTC));
    }
}
