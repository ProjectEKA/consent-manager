package in.projecteka.consentmanager.dataflow.model;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class GatewayDataFlowRequest {
    UUID requestId;
    String timestamp;
    DataFlowRequest hiRequest;
}
