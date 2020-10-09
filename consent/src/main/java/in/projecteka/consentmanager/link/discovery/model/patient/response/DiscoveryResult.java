package in.projecteka.consentmanager.link.discovery.model.patient.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import in.projecteka.library.clients.model.GatewayResponse;
import in.projecteka.library.clients.model.RespError;
import lombok.Builder;
import lombok.Value;
import org.springframework.util.StringUtils;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DiscoveryResult {
    UUID requestId;
    LocalDateTime timestamp;
    UUID transactionId;
    Patient patient;
    RespError error;
    @NotNull
    GatewayResponse resp;

    public boolean hasResponseId() {
        return (resp != null) && !StringUtils.isEmpty(resp.getRequestId());
    }
}
