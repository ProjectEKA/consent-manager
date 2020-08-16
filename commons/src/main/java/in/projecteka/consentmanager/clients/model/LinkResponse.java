package in.projecteka.consentmanager.clients.model;

import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class LinkResponse {
    UUID requestId;
    LocalDateTime timestamp;
    Acknowledgement acknowledgement;
    RespError error;
    @NotNull
    GatewayResponse resp;
}