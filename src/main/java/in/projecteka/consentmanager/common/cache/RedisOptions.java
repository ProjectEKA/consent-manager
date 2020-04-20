package in.projecteka.consentmanager.common.cache;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConditionalOnProperty(value="consentmanager.cacheMethod", havingValue = "redis")
@Configuration
@ConfigurationProperties(prefix = "consentmanager.redis")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor
public class RedisOptions {
    private String host;
    private int port;
    private String password;
}
