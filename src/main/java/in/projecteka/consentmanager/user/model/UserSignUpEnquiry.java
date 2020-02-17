package in.projecteka.consentmanager.user.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserSignUpEnquiry {
    private String identifierType;
    private String identifier;
}
