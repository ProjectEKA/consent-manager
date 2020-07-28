package in.projecteka.consentmanager.link.link.model;

import lombok.AllArgsConstructor;
import lombok.Value;

@AllArgsConstructor
@Value
public class TokenConfirmation {
    String linkRefNumber;
    String token;
}
