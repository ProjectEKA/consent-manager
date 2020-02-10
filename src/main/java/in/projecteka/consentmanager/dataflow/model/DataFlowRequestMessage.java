package in.projecteka.consentmanager.dataflow.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DataFlowRequestMessage {
    private String transactionId;
    private DataFlowRequest dataFlowRequest;
}
