package in.projecteka.consentmanager.link.discovery.model.patient.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import in.projecteka.consentmanager.clients.model.RespError;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Value
@Builder
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DiscoveryResult {
    private UUID requestId;
    private String timestamp;
    private UUID transactionId;
    private Patient patient;
    private RespError error;
    @NotNull
    private GatewayResponse resp;

    public boolean hasResponseId(){
        if (resp == null) {
            return false;
        }
        if (resp.getRequestId() == null || "".equals(resp.getRequestId())) {
            return false;
        }
        return true;
    }
}
