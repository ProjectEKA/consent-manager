package in.org.projecteka.hdaf.link.link.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@Builder
@NoArgsConstructor
@Data
public class PatientLinkReferenceResponse {
    private String transactionId;
    private Link link;
}
