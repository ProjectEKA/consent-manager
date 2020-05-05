package in.projecteka.consentmanager.user.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class OtpVerificationResponse {
    private String sessionId;
    private String mobile;
    private int expirationTime;
}
