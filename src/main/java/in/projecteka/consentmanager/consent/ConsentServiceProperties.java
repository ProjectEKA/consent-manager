package in.projecteka.consentmanager.consent;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "consentmanager.consentservice")
@Getter
@AllArgsConstructor
@ConstructorBinding
public class ConsentServiceProperties {
    private final int maxPageSize;
    private final int defaultPageSize;
    private final String url;
}
