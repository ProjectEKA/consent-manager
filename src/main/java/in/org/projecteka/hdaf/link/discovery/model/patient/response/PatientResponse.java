package in.org.projecteka.hdaf.link.discovery.model.patient.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@JsonIgnoreProperties
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class PatientResponse {
    private Patient patient;
    private String transactionId;
}