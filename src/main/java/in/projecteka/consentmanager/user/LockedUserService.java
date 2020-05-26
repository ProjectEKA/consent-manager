package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.user.model.LockedUser;
import in.projecteka.consentmanager.user.model.OtpAttempt;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

@AllArgsConstructor
public class LockedUserService {
    private final LockedUsersRepository lockedUsersRepository;
    private final LockedServiceProperties lockedServiceProperties;

    public Mono<Void> validateLogin(String cmId){
        return lockedUsersRepository.getLockedUserFor(cmId)
                .filter(lockedUser -> lockedUser.getInvalidAttempts() >= lockedServiceProperties.getMaximumInvalidAttempts())
                .flatMap(lockedUser -> {
                    var isBlocked = isWithinTimeLimit(lockedUser, lockedServiceProperties.getCoolOfPeriod());
                    return isBlocked ? Mono.error(ClientError.userBlocked()) : removeLockedUser(cmId);
                });
    }

    public Mono<Void> removeLockedUser(String cmId){
        return lockedUsersRepository.deleteUser(cmId);
    }

    public Mono<Void> createOrUpdateLockedUser(String cmId){
        return lockedUsersRepository.upsert(cmId);
    }

    private boolean isWithinTimeLimit(LockedUser user, int timeLimitInMin) {
        return user
                .getDateModified()
                .plusMinutes(timeLimitInMin)
                .isAfter(LocalDateTime.now(ZoneOffset.UTC));
    }
}
