package in.projecteka.consentmanager.user.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
public class RegistrationRequest {
    private final String phoneNumber;
    private final boolean blockedStatus;
}
