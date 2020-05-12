package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.user.model.LockedUser;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@AllArgsConstructor
public class LockedUserService {
    private final LockedUsersRepository lockedUsersRepository;
    private final LockedServiceProperties lockedServiceProperties;
    private final Logger logger = LoggerFactory.getLogger(LockedUserService.class);

    public Mono<LockedUser> userFor(String patientId) {
        logger.debug("Invoking repository go get the locked user for patientId " + patientId);
        return lockedUsersRepository.getLockedUserFor(patientId);
    }

    public Mono<Void> createUser(String patientId) {
        var lockedUser = new LockedUser(1, patientId, false, null, new Date().toString());
        return lockedUsersRepository.insert(lockedUser);
    }

    public boolean isUserBlocked(LockedUser user) {
        return user != null &&
                (!isAfterEightHours(user.getFirstInvalidAttemptTime())
                        || !isAfterEightHours(user.getLockedTime())) && user.getIsLocked();
    }

    public Mono<ClientError> validateAndUpdate(LockedUser user) {
        var maximumInvalidAttempts = lockedServiceProperties.getMaximumInvalidAttempts();
        var blockedTime = user.getLockedTime();
        var firstInvalidAttempt = user.getFirstInvalidAttemptTime();

        if (isAfterEightHours(firstInvalidAttempt) || isAfterEightHours(blockedTime)) {
            return reAddUser(user.getPatientId());
        }

        if (user.getInvalidAttempts() == maximumInvalidAttempts) {
            return blockUser(user.getPatientId());
        }

        if (user.getInvalidAttempts() < maximumInvalidAttempts) {
            return updateUserAndReturnError(user, ClientError.unAuthorizedRequest("Username or password is incorrect"));
        } else {
            return updateUserAndReturnError(user, ClientError.userBlocked());
        }
    }


    private Mono<ClientError> updateUserAndReturnError(LockedUser user, ClientError error) {
        return lockedUsersRepository
                .updateUser(user.getIsLocked(),
                        user.getLockedTime(),
                        user.getPatientId(),
                        user.getInvalidAttempts() + 1)
                .thenReturn(error);
    }

    private Mono<ClientError> reAddUser(String patientId) {
        return lockedUsersRepository
                .deleteUser(patientId)
                .then(createUser(patientId)
                        .thenReturn(ClientError.unAuthorizedRequest("Username or password is incorrect")));
    }

    private Mono<ClientError> blockUser(String patientId) {
        var maximumInvalidAttempts = lockedServiceProperties.getMaximumInvalidAttempts();
        return lockedUsersRepository.updateUser(true,
                new Date().toString(),
                patientId,
                maximumInvalidAttempts + 1)
                .thenReturn(ClientError.userBlocked());
    }

    public boolean isAfterEightHours(String time) {
        if (time == null || time.equals("")) {
            return false;
        }

        var coolOfPeriod = lockedServiceProperties.getCoolOfPeriod();
        var milliToHour = 1000 * 60 * 60;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
            Date now = sdf.parse(new Date().toString());
            Date pastDate = sdf.parse(time);
            return ((now.getTime() - pastDate.getTime()) / milliToHour) >= coolOfPeriod;
        } catch (ParseException e) {
            return false;
        }
    }
}
