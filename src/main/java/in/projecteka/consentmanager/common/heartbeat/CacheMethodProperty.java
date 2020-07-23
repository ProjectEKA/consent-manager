package in.projecteka.consentmanager.common.heartbeat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "consentmanager.cachemethod")
@Getter
@AllArgsConstructor
@ConstructorBinding
public class CacheMethodProperty {
    private final String methodName;
}