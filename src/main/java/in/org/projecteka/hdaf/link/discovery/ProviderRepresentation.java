package in.org.projecteka.hdaf.link.discovery;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;

@AllArgsConstructor
@Getter
@Value
@Builder
public class ProviderRepresentation {
    private String name;
    private String city;
    private String telephone;
    private String type;
}
