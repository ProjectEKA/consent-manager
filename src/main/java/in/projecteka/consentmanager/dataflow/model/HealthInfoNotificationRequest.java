package in.projecteka.consentmanager.dataflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
public class HealthInfoNotificationRequest {
    private String transactionId;
    private String consentId;
    private Date doneAt;
    private Notifier notifier;
    private StatusNotification statusNotification;
}
