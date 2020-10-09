package in.projecteka.consentmanager.userauth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import javax.validation.Valid;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class UserAuthConfirmRequest {
    private final String requestId;
    private final LocalDateTime timestamp;
    @NonNull
    @Valid
    private final String transactionId;
    private final AuthCredentialDetail credential;
}
