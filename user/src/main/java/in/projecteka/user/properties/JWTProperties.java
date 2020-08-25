package in.projecteka.user.properties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "user.jwt")
@AllArgsConstructor
@ConstructorBinding
@Getter
public class JWTProperties {
    private final String secret;
}
