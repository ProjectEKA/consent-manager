package in.projecteka.hdaf.link.discovery.model.patient.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@AllArgsConstructor
@Value
@Builder
public class Identifier {
    private String type;
    private String value;
}
