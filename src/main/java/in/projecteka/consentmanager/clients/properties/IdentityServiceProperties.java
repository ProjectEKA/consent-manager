package in.projecteka.consentmanager.clients.properties;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Builder
@Configuration
@ConfigurationProperties(prefix = "consentmanager.keycloak")
@Data
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor
public class IdentityServiceProperties {
    private String baseUrl;
    private String clientId;
    private String clientSecret;
    private String userName;
    private String password;
}
