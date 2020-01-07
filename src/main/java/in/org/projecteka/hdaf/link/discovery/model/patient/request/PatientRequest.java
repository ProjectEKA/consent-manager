package in.org.projecteka.hdaf.link.discovery.model.patient.request;

import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class PatientRequest {
    private Patient patient;
    private String transactionId;
}
