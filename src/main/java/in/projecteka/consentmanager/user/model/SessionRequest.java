package in.projecteka.consentmanager.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class SessionRequest {

    private final GrantType grantType;

    private final String UserName;

    private final String Password;
}
