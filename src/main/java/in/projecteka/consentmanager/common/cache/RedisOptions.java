package in.projecteka.consentmanager.common.cache;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConditionalOnProperty(value="consentmanager.cacheMethod", havingValue = "redis")
@ConfigurationProperties(prefix = "consentmanager.redis")
@Getter
@AllArgsConstructor
@ConstructorBinding
public class RedisOptions {
    private String host;
    private int port;
    private String password;
}
