package in.org.projecteka.hdaf.link.discovery.model.patient;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;

@AllArgsConstructor
@Getter
@Value
public class Identifier {
    private String type;
    private String value;
}
