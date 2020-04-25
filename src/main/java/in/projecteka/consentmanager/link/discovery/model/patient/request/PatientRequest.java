package in.projecteka.consentmanager.link.discovery.model.patient.request;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class PatientRequest {
    Patient patient;
    String requestId;
}
