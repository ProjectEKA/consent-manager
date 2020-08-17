package in.projecteka.consentmanager.properties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "consentmanager.cachemethod")
@Getter
@AllArgsConstructor
@ConstructorBinding
@Builder
public class CacheMethodProperty {
    private final String methodName;

    public in.projecteka.library.common.heartbeat.CacheMethodProperty toHeartBeat() {
        return new in.projecteka.library.common.heartbeat.CacheMethodProperty(methodName);
    }
}