package in.projecteka.consentmanager.link.discovery.model.patient.request;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class PatientRequest {
    private Patient patient;
    private String transactionId;
}
