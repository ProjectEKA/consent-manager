package in.projecteka.dataflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder(toBuilder = true)
public class DataFlowRequest {
    private final Consent consent;
    private final DateRange dateRange;
    private final String dataPushUrl;
    private final KeyMaterial keyMaterial;
}
