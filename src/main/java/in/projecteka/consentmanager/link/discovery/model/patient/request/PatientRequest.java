package in.projecteka.consentmanager.link.discovery.model.patient.request;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Builder
@Value
public class PatientRequest {
    Patient patient;
    UUID requestId;
}
