package in.projecteka.consentmanager.properties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "consentmanager.linkservice")
@Getter
@AllArgsConstructor
@ConstructorBinding
public class LinkServiceProperties {
    private final String url;
    private final int txnTimeout;
}
