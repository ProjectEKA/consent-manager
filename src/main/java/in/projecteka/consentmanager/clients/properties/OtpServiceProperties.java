package in.projecteka.consentmanager.clients.properties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;

@ConfigurationProperties(prefix = "consentmanager.otpservice")
@AllArgsConstructor
@ConstructorBinding
@Getter
public class OtpServiceProperties {
    private final String url;
    private final List<String> identifiers;
    private final int expiryInMinutes;
    private final List<String> allowListNumbers;

    public List<String> getIdentifiers() {
        return Optional.ofNullable(identifiers).orElse(new ArrayList<>());
    }

    public List<String> allowListNumbers() {
        return Optional.ofNullable(allowListNumbers).orElse(asList("+91-8888888888", "+91-9999999999"));
    }
}
