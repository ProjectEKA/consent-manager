package in.projecteka.consentmanager.properties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "consentmanager.listeners")
@AllArgsConstructor
@Getter
@ConstructorBinding
public class ListenerProperties {
    private final int maximumRetries;
    private final int retryInterval;
}
