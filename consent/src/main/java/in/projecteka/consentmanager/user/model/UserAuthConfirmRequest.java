package in.projecteka.consentmanager.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class UserAuthConfirmRequest {
    private final String requestId;
    private final LocalDateTime timestamp;
    private final String transactionId;
    private final AuthCredentialDetail credential;
}
