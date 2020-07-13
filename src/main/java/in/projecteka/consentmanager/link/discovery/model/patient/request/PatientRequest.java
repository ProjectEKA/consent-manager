package in.projecteka.consentmanager.link.discovery.model.patient.request;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Value
public class PatientRequest {
    Patient patient;
    UUID requestId;
    LocalDateTime timestamp;
    UUID transactionId;
}
