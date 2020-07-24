package in.projecteka.consentmanager.user.model;

import lombok.Data;

@Data
public class AuthConfirmationDetail {
    private final String transactionId;
    private final String token;
}
