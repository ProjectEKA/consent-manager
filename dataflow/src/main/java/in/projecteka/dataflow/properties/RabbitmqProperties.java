package in.projecteka.dataflow.properties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "spring.rabbitmq")
@Getter
@AllArgsConstructor
@ConstructorBinding
public class RabbitmqProperties {
    public final String host;
    public final int port;
    public final String username;
    public final String password;
}
