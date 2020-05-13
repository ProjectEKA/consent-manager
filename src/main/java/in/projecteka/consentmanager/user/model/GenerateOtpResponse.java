package in.projecteka.consentmanager.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
@Getter
public class GenerateOtpResponse {
    private String sessionId;
    private String otpMedium;
    private String otpMediumValue;
    private int expiryInMinutes;
}
