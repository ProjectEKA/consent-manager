package in.projecteka.consentmanager.clients.properties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "consentmanager.otpservice")
@AllArgsConstructor
@ConstructorBinding
@Getter
public class OtpServiceProperties {
    private String url;
    private List<String> identifiers = new ArrayList<>();
}
