package in.projecteka.consentmanager.user;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "consentmanager.keycloak")
@Data
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor
public class KeycloakProperties {
    private String baseUrl;
    private String clientId;
    private String clientSecret;
    private String userName;
    private String password;
}
