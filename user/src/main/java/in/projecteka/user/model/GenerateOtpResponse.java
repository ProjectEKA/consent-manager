package in.projecteka.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
@Getter
public class GenerateOtpResponse {
    private final String sessionId;
    private final String otpMedium;
    private final String otpMediumValue;
    private final int expiryInMinutes;
}
