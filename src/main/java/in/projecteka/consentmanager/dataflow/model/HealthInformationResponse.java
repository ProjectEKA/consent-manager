package in.projecteka.consentmanager.dataflow.model;

import in.projecteka.consentmanager.clients.model.RespError;
import in.projecteka.consentmanager.link.discovery.model.patient.response.GatewayResponse;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Value
@Builder
public class HealthInformationResponse {
    UUID requestId;
    String timestamp;
    AcknowledgementResponse hiRequest;
    @NotNull
    GatewayResponse resp;
    RespError error;
}
