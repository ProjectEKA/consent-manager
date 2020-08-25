package in.projecteka.consentmanager.link.hiplink.model.request;

import in.projecteka.consentmanager.user.model.AuthCredentialDetail;
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
