package in.projecteka.consentmanager.consent;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "consentmanager.nhsproperties")
@Getter
@AllArgsConstructor
@ConstructorBinding
public class NHSProperties {
    private final String hiuId;
}
