package in.projecteka.consentmanager.consent;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "consentmanager.consentservice")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor
public class ConsentServiceProperties {
    private int maxPageSize;
    private int defaultPageSize;
    private String url;
    private String purposeOfUseDefUrl;
    private String hiTypesDefUrl;
}
