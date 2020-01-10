package in.projecteka.hdaf.link.discovery;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@AllArgsConstructor
@Value
@Builder
public class IdentifierRepresentation {
    private String name;
    private String id;
}
