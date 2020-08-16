package in.projecteka.consentmanager.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "consentmanager.listeners")
@AllArgsConstructor
@Getter
@ConstructorBinding
public class ListenerProperties {
    private int maximumRetries;
    private int retryInterval;
}
