package in.projecteka.consentmanager.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@AllArgsConstructor
@Builder
@Data
public class ForgotPinOTPSession {
    private final String sessionId;
}
