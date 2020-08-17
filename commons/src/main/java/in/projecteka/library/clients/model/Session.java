package in.projecteka.library.clients.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Session {

    @JsonAlias({"access_token"})
    private String accessToken;

    @JsonAlias({"expires_in"})
    private int expiresIn;

    @JsonAlias({"refresh_expires_in"})
    private int refreshExpiresIn;

    @JsonAlias({"refresh_token"})
    private String refreshToken;

    @JsonAlias({"token_type"})
    private String tokenType;
}
