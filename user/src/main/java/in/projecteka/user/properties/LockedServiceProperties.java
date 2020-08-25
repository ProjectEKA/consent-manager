package in.projecteka.user.properties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "user.locked-user-service")
@Getter
@AllArgsConstructor
@ConstructorBinding
public class LockedServiceProperties {
    private final int maximumInvalidAttempts;
    private final int coolOfPeriod;
}
