package in.projecteka.consentmanager.consent.model.request;

import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class ConsentRequestStatus {
    UUID requestId;
    LocalDateTime timestamp;
    @NotBlank(message = "consent request id is not specified")
    String consentRequestId;
}