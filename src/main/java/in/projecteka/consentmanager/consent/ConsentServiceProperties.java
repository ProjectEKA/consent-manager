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
    private static final int DEFAULT_MAX_PAGE_SIZE = 50;
    private final int maxPageSize;
    private final int defaultPageSize;
    private final String url;
    private final int consentRequestExpiry;
    private final String purposeOfUseDefUrl;
    private final String hiTypesDefUrl;
    private final String name;

    public int getMaxPageSize() {
        return maxPageSize > 0 ? maxPageSize : DEFAULT_MAX_PAGE_SIZE;
    }

    public int getDefaultPageSize() {
        return defaultPageSize > 0 ? defaultPageSize : DEFAULT_MAX_PAGE_SIZE;
    }
}
