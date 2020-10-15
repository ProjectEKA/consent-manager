package in.projecteka.consentmanager.consent.model.response;

import in.projecteka.consentmanager.consent.model.ConsentStatusDetail;
import in.projecteka.library.clients.model.GatewayResponse;
import in.projecteka.library.clients.model.RespError;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class ConsentStatusResponse {
    UUID requestId;
    LocalDateTime timestamp;
    ConsentStatusDetail consentRequest;
    RespError error;
    @NotNull
    GatewayResponse resp;
}
