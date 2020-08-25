package in.projecteka.user.model;

import in.projecteka.library.clients.model.Meta;
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
