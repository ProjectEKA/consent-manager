package in.projecteka.consentmanager.clients.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
public class KeycloakUser {
    private String firstName;
    private String username;
    private List<UserCredential> credentials;
    private String enabled;
}
