package in.projecteka.consentmanager.consent.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import in.projecteka.consentmanager.clients.model.RespError;
import in.projecteka.consentmanager.link.discovery.model.patient.response.GatewayResponse;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Builder
@Data
public class ConsentRequestResult {
    UUID requestId;
    String timestamp;
    ConsentRequestId consentRequest;
    private RespError error;
    @NotNull
    private GatewayResponse resp;
}
