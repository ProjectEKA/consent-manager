package in.projecteka.consentmanager.link.discovery;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@AllArgsConstructor
@Value
@Builder
public class ProviderRepresentation {
    IdentifierRepresentation identifier;
    String city;
    String telephone;
    String type;
}
