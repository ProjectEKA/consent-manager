package in.projecteka.consentmanager.userauth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
@AllArgsConstructor
public class AuthInitRequest {
    UUID requestId;
    LocalDateTime timestamp;
    @NonNull
    @Valid
    Query query;
}
