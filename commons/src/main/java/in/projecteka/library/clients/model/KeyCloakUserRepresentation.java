package in.projecteka.library.clients.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
@Getter
public class KeyCloakUserRepresentation {
    private final String id;
}
