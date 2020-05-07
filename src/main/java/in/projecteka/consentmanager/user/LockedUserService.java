package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.user.model.LockedUser;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Date;

@AllArgsConstructor
public class LockedUserService {
    private final LockedUsersRepository lockedUsersRepository;
    private final Logger logger = LoggerFactory.getLogger(LockedUserService.class);

    public Mono<LockedUser> userFor(String patientId) {
        logger.info("Invoking repository go get the locked user for patientId " + patientId);
        return lockedUsersRepository.getLockedUserFor(patientId);
    }

    public Mono<Void> createUser(String patientId) {
        return lockedUsersRepository.insert(new LockedUser(1, patientId, false, null));
    }

    public Mono<ClientError> updateUser(LockedUser user) {
        var isLocked = user.getIsLocked();
        var blockedTime = user.getLockedTime();
        var clientError = ClientError.unAuthorizedRequest();
        if (user.getInvalidAttempts() >= 5) {
            if (user.getInvalidAttempts() == 5) {
                blockedTime = new Date().toString();
            }
            isLocked = true;
            clientError = ClientError.userBlocked();
        }
        return lockedUsersRepository.updateUser(isLocked, blockedTime,
                user.getPatientId(), user.getInvalidAttempts() + 1).thenReturn(clientError);
    }
}
