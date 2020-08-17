package in.projecteka.consentmanager.consent.model.response;

import in.projecteka.consentmanager.link.discovery.model.patient.response.GatewayResponse;
import in.projecteka.library.clients.model.RespError;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Data
public class ConsentRequestResult {
    private UUID requestId;
    private LocalDateTime timestamp;
    private ConsentRequestId consentRequest;
    private RespError error;
    @NotNull
    private GatewayResponse resp;
}
