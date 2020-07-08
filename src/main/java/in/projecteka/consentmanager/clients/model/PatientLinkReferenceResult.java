package in.projecteka.consentmanager.clients.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import in.projecteka.consentmanager.link.discovery.model.patient.response.GatewayResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class PatientLinkReferenceResult {
    private UUID requestId;
    private String timestamp;
    private UUID transactionId;
    private Link link;
    private RespError error;
    @NotNull
    private GatewayResponse resp;

    public boolean hasResponseId() {
        return (resp != null) && !StringUtils.isEmpty(resp.getRequestId());
    }
}