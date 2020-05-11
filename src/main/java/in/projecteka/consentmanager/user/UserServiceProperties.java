package in.projecteka.consentmanager.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "consentmanager.userservice")
@Getter
@AllArgsConstructor
@ConstructorBinding
public class UserServiceProperties {
    private final String url;
    private final int transactionPinDigitSize;
    private final int transactionPinTokenValidity;
    private final int userCreationTokenValidity;
    private final String userIdSuffix;
    private final long maxIncorrectPinAttempts;
}
