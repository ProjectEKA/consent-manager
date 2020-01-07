package in.org.projecteka.hdaf.link.link.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.*;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@AllArgsConstructor
@Builder
@NoArgsConstructor
@Setter
public class PatientInHIP {
    private String consentManagerUserID;
    @JsonUnwrapped
    private Patient patient;
}
