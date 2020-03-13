package in.projecteka.consentmanager.user.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransactionPin {
    private String pin;
    private String patientId;
}
