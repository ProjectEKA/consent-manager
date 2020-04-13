package in.projecteka.consentmanager.clients.model;

import in.projecteka.consentmanager.user.model.UserCredential;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
public class KeycloakUser {
    private String name;
    private String username;
    private List<UserCredential> credentials;
    private String enabled;
}
