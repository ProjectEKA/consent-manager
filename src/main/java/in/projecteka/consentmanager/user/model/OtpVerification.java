package in.projecteka.consentmanager.user.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class OtpVerification {
    private final String sessionId;
    private final String value;
}
