package in.projecteka.consentmanager.clients.properties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "consentmanager.linkservice")
@Getter
@AllArgsConstructor
@ConstructorBinding
public class LinkServiceProperties {
    private String url;
}
