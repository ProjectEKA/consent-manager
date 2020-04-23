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
    private String url;
    private int transactionPinDigitSize;
    private int transactionPinTokenValidity;
    private int userCreationTokenValidity;
    private String userIdSuffix;
}
