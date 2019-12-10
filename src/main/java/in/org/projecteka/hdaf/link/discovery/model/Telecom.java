package in.org.projecteka.hdaf.link.discovery.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@AllArgsConstructor
@Getter
@NoArgsConstructor
@Setter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Telecom {
    private String use;
    private String value;
}
