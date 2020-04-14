package in.projecteka.consentmanager.common.cache;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("redis")
@Configuration
@ConfigurationProperties(prefix = "consentmanager.redis")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor
public class RedisOptions {
    private String host;
    private int port;
}
