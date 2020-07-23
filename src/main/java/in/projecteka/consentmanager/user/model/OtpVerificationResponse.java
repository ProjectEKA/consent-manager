package in.projecteka.consentmanager.user.model;

import in.projecteka.consentmanager.clients.model.Meta;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class OtpVerificationResponse {
    private String sessionId;
    private Meta meta;
}
