package in.projecteka.consentmanager.user.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class PatientRequest {
    private UUID requestId;
    private LocalDateTime timestamp;
    private PatientQuery query;
}
