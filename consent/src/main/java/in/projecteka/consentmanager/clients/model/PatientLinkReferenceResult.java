package in.projecteka.consentmanager.clients.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import in.projecteka.consentmanager.link.discovery.model.patient.response.GatewayResponse;
import in.projecteka.library.clients.model.RespError;
import lombok.Builder;
import lombok.Value;
import org.springframework.util.StringUtils;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Builder
@Value
public class PatientLinkReferenceResult {
    UUID requestId;
    LocalDateTime timestamp;
    UUID transactionId;
    Link link;
    RespError error;
    @NotNull
    GatewayResponse resp;

    public boolean hasResponseId() {
        return (resp != null) && !StringUtils.isEmpty(resp.getRequestId());
    }
}