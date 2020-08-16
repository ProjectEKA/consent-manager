package in.projecteka.consentmanager.clients.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
@Getter
public class KeyCloakUserCredentialRepresentation {
    private final String id;
    private final String type;
}
