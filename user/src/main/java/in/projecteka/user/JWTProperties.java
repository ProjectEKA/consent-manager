package in.projecteka.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "consentmanager.jwt")
@AllArgsConstructor
@ConstructorBinding
@Getter
public class JWTProperties {
    private final String secret;
}
