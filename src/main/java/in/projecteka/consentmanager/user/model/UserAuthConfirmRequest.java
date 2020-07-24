package in.projecteka.consentmanager.user.model;

import lombok.Data;

@Data
public class UserAuthConfirmRequest {
    private final String requestId;
    private final String timestamp;
    private final AuthConfirmationDetail confirmation;
}
