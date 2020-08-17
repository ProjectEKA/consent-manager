package in.projecteka.consentmanager.link.link.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import in.projecteka.consentmanager.clients.model.PatientRepresentation;
import in.projecteka.consentmanager.link.discovery.model.patient.response.GatewayResponse;
import in.projecteka.library.clients.model.RespError;
import lombok.Builder;
import lombok.Value;
import org.springframework.util.StringUtils;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LinkConfirmationResult {
    UUID requestId;
    LocalDateTime timestamp;
    PatientRepresentation patient;
    RespError error;
    @NotNull
    GatewayResponse resp;

    public boolean hasResponseId() {
        return (resp != null) && !StringUtils.isEmpty(resp.getRequestId());
    }
}
