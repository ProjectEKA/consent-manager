package in.projecteka.dataflow.model.hip;

import in.projecteka.dataflow.model.Consent;
import in.projecteka.dataflow.model.DateRange;
import in.projecteka.dataflow.model.KeyMaterial;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;


@Getter
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class HiRequest {
    private final Consent consent;
    private final DateRange dateRange;
    private final String dataPushUrl;
    private final KeyMaterial keyMaterial;
}
