package in.projecteka.consentmanager.hipLinkInitialization.model.request;

import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
public class AuthInitRequest {
    UUID requestId;
    LocalDateTime timestamp;
    Query query;
}
