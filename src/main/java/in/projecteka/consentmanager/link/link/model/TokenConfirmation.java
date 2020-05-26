package in.projecteka.consentmanager.link.link.model;

import lombok.AllArgsConstructor;
import lombok.Value;

@AllArgsConstructor
@Value
public class TokenConfirmation {
    private String linkRefNumber;
    private String token;
}
