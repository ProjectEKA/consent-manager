package in.projecteka.consentmanager.dataflow.model;

import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotEmpty;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
public class GatewayDataFlowRequest {
    UUID requestId;
    String timestamp;
    @NotEmpty(message = "hiRequest can't be null")
    DataFlowRequest hiRequest;
}
