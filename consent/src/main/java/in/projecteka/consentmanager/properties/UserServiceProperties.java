package in.projecteka.consentmanager.properties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "consentmanager.userservice")
@Getter
@AllArgsConstructor
@ConstructorBinding
@Builder
public class UserServiceProperties {
    private final String url;
}