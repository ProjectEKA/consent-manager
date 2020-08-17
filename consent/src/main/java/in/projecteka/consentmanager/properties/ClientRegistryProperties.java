package in.projecteka.consentmanager.properties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "consentmanager.clientregistry")
@AllArgsConstructor
@Getter
@ConstructorBinding
public class ClientRegistryProperties {
    private final String url;
}
