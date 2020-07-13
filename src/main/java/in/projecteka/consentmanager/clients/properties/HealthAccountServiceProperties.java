package in.projecteka.consentmanager.clients.properties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

import java.util.List;

@ConfigurationProperties(prefix = "consentmanager.healthaccountservice")
@AllArgsConstructor
@ConstructorBinding
@Getter
public class HealthAccountServiceProperties {
    private final boolean isEnabled;
    private final String url;
    private final List<String> identifiers;
    private final int expiryInMinutes;
}
