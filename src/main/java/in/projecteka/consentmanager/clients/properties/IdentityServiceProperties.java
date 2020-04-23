package in.projecteka.consentmanager.clients.properties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@Builder
@ConfigurationProperties(prefix = "consentmanager.keycloak")
@AllArgsConstructor
@ConstructorBinding
@Getter
public class IdentityServiceProperties {
    private String baseUrl;
    private String clientId;
    private String clientSecret;
    private String userName;
    private String password;
    private String jwkUrl;
    private String issuer;
}
