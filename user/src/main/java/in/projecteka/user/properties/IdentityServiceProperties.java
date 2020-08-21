package in.projecteka.user.properties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@Builder
@ConfigurationProperties(prefix = "user.keycloak")
@AllArgsConstructor
@ConstructorBinding
@Getter
public class IdentityServiceProperties {
    private final String baseUrl;
    private final String clientId;
    private final String clientSecret;
    private final String userName;
    private final String password;
    private final String jwkUrl;
    private final String issuer;
}
