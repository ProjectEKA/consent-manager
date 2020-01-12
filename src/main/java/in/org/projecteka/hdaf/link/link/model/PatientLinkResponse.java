package in.org.projecteka.hdaf.link.link.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@Builder
@NoArgsConstructor
@Data
public class PatientLinkResponse {
    private PatientRepresentation patient;
}
