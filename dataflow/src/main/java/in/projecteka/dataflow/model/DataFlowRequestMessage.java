package in.projecteka.dataflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
@Getter
public class DataFlowRequestMessage {
    private final String transactionId;
    private final DataFlowRequest dataFlowRequest;
}
