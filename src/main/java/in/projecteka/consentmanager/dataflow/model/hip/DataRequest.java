package in.projecteka.consentmanager.dataflow.model.hip;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class DataRequest {
    private final String transactionId;
    private final UUID requestId;
    private final String timestamp;
    private final HiRequest hiRequest;
}
