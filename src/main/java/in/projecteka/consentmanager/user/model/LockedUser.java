package in.projecteka.consentmanager.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Date;


@Builder
@AllArgsConstructor
@Getter
public class LockedUser {
    private final int invalidAttempts;
    private final String patientId;
    @Builder.Default
    private final Boolean isLocked = false;
    @Builder.Default
    private final Date lockedTime = null;
}

