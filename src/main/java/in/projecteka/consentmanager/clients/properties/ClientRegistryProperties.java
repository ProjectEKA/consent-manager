package in.projecteka.consentmanager.clients.properties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "consentmanager.clientregistry")
@AllArgsConstructor
@Getter
@ConstructorBinding
public class ClientRegistryProperties {
    private String url;
    private String XAuthToken;
    private String clientId;
    private String jwkUrl;
}
