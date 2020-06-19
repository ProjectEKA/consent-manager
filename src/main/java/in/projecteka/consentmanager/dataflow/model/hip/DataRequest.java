package in.projecteka.consentmanager.dataflow.model.hip;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class DataRequest {
    private final UUID transactionId;
    private final UUID requestId;
    private final LocalDateTime timestamp;
    private final HiRequest hiRequest;
}
