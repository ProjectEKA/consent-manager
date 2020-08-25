package in.projecteka.user.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class PatientRequest {
    UUID requestId;
    LocalDateTime timestamp;
    PatientQuery query;
}
