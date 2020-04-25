package in.projecteka.consentmanager.link.discovery.model.patient.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PatientResponse {
    Patient patient;
}