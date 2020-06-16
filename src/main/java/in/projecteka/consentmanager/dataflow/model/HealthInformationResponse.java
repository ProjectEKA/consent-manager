package in.projecteka.consentmanager.dataflow.model;

import in.projecteka.consentmanager.clients.model.RespError;
import in.projecteka.consentmanager.link.discovery.model.patient.response.GatewayResponse;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class HealthInformationResponse {
    UUID requestId;
    String timestamp;
    AcknowledgementResponse hiRequest;
    GatewayResponse resp;
    RespError error;
}
