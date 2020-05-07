package in.projecteka.consentmanager.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "consentmanager.lockeduserservice")
@Getter
@AllArgsConstructor
@ConstructorBinding
public class LockedServiceProperties {
    private final int maximumInvalidAttempts;
}
