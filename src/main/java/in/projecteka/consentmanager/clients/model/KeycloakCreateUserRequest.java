package in.projecteka.consentmanager.clients.model;

import in.projecteka.consentmanager.user.model.UserCredential;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
public class KeycloakCreateUserRequest {
    private String firstName;
    private String lastName;
    private String username;
    private List<UserCredential> credentials;
    private String enabled;
}
