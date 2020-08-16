package in.projecteka.library.common.heartbeat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "spring.rabbitmq")
@Getter
@AllArgsConstructor
@ConstructorBinding
public class RabbitmqOptions {
    private final String host;
    private final int port;
    private final String username;
    private final String password;
}
