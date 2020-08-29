package in.projecteka.dataflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
public class Notification {
    private String consentId;
    private String transactionId;
    private LocalDateTime doneAt;
    private Notifier notifier;
    private StatusNotification statusNotification;
}
