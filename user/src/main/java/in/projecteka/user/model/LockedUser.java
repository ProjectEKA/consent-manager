package in.projecteka.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;


@Builder
@AllArgsConstructor
@Getter
public class LockedUser {
    private final int invalidAttempts;
    private final String patientId;
    @Builder.Default
    private final boolean isLocked = false;
    private final LocalDateTime dateModified;
    private final LocalDateTime dateCreated;
}

