package in.projecteka.consentmanager.dataflow.model;

import in.projecteka.consentmanager.link.discovery.model.patient.response.GatewayResponse;
import in.projecteka.library.clients.model.RespError;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Value
public class DataFlowRequestResult {
    UUID requestId;
    LocalDateTime timestamp;
    HIRequest hiRequest;
    RespError error;
    GatewayResponse resp;
}