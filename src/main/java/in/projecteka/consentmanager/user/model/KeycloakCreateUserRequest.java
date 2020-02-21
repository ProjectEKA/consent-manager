package in.projecteka.consentmanager.user.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class KeycloakCreateUserRequest {
    private String firstName;
    private String lastName;
    private String username;
    private List<UserCredential> credentials;
    private String enabled;
}
