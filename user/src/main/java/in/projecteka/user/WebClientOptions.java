package in.projecteka.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConstructorBinding
@ConfigurationProperties("webclient.connection-pool")
@Getter
@AllArgsConstructor
public class WebClientOptions {
    private final int poolSize;
    private final int maxLifeTime;
    private final int maxIdleTimeout;
}
