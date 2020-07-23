package in.projecteka.consentmanager.user.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SessionRequest {
    GrantType grantType;

    String username;

    String password;

    String refreshToken;
}
