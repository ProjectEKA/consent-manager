package in.projecteka.consentmanager.user.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class OtpPermitRequest {
    private final String username;
    private final String sessionId;
    private final String otp;
}
