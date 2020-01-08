package in.org.projecteka.hdaf.link.discovery;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@AllArgsConstructor
@Value
@Builder
public class ProviderRepresentation {
    private IdentifierRepresentation identifier;
    private String city;
    private String telephone;
    private String type;
}
