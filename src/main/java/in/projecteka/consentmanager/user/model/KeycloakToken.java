package in.projecteka.consentmanager.user.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeycloakToken {

    @JsonAlias({ "access_token" })
    private String accessToken;

    @JsonAlias({ "expires_in" })
    private int expiresIn;

    @JsonAlias({ "refresh_expires_in" })
    private int refreshExpiresIn;

    @JsonAlias({ "refresh_token" })
    private String refreshToken;

    @JsonAlias({ "token_type" })
    private String tokenType;
}
