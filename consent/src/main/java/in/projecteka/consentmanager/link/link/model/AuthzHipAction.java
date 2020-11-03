package in.projecteka.consentmanager.link.link.model;

import in.projecteka.consentmanager.userauth.model.RequesterType;
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
    String requesterId;
    String patientId;
    String purpose;
    LocalDateTime expiry;
    Integer repeat;
    Integer currentCounter;
    RequesterType requesterType;
}