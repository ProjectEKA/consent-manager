package in.projecteka.consentmanager.link.link.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class TokenConfirmation {
    private String linkRefNumber;
    private String token;
}
