package in.projecteka.consentmanager.user.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
public class OtpAttempt {
    private final String phoneNumber;
    private final boolean blocked;
    private final LocalDateTime timestamp;
}
