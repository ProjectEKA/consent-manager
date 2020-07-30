package in.projecteka.consentmanager.link.link.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@Value
@Builder
public class LinkRequest {
    @NotNull
    UUID requestId;
    @NotNull
    LocalDateTime timestamp;
    @Valid
    Link link;
}