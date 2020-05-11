package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.user.model.LockedUser;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.text.SimpleDateFormat;
import java.util.Date;

@AllArgsConstructor
public class LockedUserService {
    private final LockedUsersRepository lockedUsersRepository;
    private final LockedServiceProperties lockedServiceProperties;
    private final Logger logger = LoggerFactory.getLogger(LockedUserService.class);

    public Mono<LockedUser> userFor(String patientId) {
        logger.info("Invoking repository go get the locked user for patientId " + patientId);
        return lockedUsersRepository.getLockedUserFor(patientId);
    }

    public Mono<Void> createUser(String patientId) {
        var lockedUser = new LockedUser(1, patientId, false, null, new Date().toString());
        return lockedUsersRepository.insert(lockedUser);
    }

    public Mono<ClientError> validateAndUpdate(LockedUser user) {
        var MAXIMUM_INVALID_PASSWORD_ATTEMPTS = lockedServiceProperties.getMaximumInvalidAttempts();
        var blockedTime = user.getLockedTime();
        var firstInvalidAttempt = user.getFirstInvalidAttempt();

        if (user.getInvalidAttempts() > MAXIMUM_INVALID_PASSWORD_ATTEMPTS && isAfterEightHours(blockedTime)) {
            return reAddUser(user.getPatientId());
        }

        if (user.getInvalidAttempts() < MAXIMUM_INVALID_PASSWORD_ATTEMPTS && isAfterEightHours(firstInvalidAttempt)) {
            return reAddUser(user.getPatientId());
        }

        if (user.getInvalidAttempts() == MAXIMUM_INVALID_PASSWORD_ATTEMPTS) {
            return blockUser(user.getPatientId());
        }

        if (user.getInvalidAttempts() < MAXIMUM_INVALID_PASSWORD_ATTEMPTS) {
            return updateUserAndReturnError(user, ClientError.unAuthorizedRequest());
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
                        .thenReturn(ClientError.unAuthorizedRequest()));
    }

    private Mono<ClientError> blockUser(String patientId) {
        var MAXIMUM_INVALID_PASSWORD_ATTEMPTS = lockedServiceProperties.getMaximumInvalidAttempts();
        return lockedUsersRepository.updateUser(true,
                new Date().toString(),
                patientId,
                MAXIMUM_INVALID_PASSWORD_ATTEMPTS + 1)
                .thenReturn(ClientError.userBlocked());
    }

    @SneakyThrows
    private boolean isAfterEightHours(String time) {
        var MAXIMUM_INVALID_PASSWORD_ATTEMPTS = lockedServiceProperties.getMaximumInvalidAttempts();
        var MILLI_TO_HOUR = 1000 * 60 * 60;
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
        Date now = sdf.parse(new Date().toString());
        Date pastDate = sdf.parse(time);
        return ((now.getTime() - pastDate.getTime()) / MILLI_TO_HOUR) >= MAXIMUM_INVALID_PASSWORD_ATTEMPTS;
    }
}
