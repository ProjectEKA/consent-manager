package in.projecteka.consentmanager.link.link.model;

import in.projecteka.library.clients.model.GatewayResponse;
import in.projecteka.library.clients.model.RespError;
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