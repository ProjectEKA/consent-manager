package in.projecteka.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ConfigurationProperties(prefix = "user.otpservice")
@AllArgsConstructor
@ConstructorBinding
@Getter
public class OtpServiceProperties {
    private final String url;
    private final List<String> identifiers;
    private final int expiryInMinutes;

    public List<String> getIdentifiers() {
        return Optional.ofNullable(identifiers).orElse(new ArrayList<>());
    }
}