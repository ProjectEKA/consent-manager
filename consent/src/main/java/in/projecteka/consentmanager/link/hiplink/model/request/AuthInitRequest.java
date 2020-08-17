package in.projecteka.consentmanager.link.hiplink.model.request;

import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
public class AuthInitRequest {
    UUID requestId;
    LocalDateTime timestamp;
    Query query;
}
