package in.projecteka.dataflow.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AcknowledgementResponse {
    String transactionId;
    DataFlowRequestStatus sessionStatus;
}
