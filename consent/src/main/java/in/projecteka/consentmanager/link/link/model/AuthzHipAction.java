package in.projecteka.consentmanager.link.link.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
public class AuthzHipAction {
    String sessionId;
    String hipId;
    String patientId;
    String purpose;
    LocalDateTime expiry;
    Integer repeat;
    Integer currentCounter;
}