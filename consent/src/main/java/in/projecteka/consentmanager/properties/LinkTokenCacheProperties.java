package in.projecteka.consentmanager.properties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "consentmanager.linktokencache")
@Getter
@AllArgsConstructor
@ConstructorBinding
public class LinkTokenCacheProperties {
    private final int expiry;
}