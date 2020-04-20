package in.projecteka.consentmanager.link.discovery;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@AllArgsConstructor
@Value
@Builder
public class IdentifierRepresentation {
    String name;
    String id;
}
