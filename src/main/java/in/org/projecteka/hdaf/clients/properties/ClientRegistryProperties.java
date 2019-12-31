package in.org.projecteka.hdaf.clients.properties;


import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "hdaf.clientregistry")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor
public class ClientRegistryProperties {
    private String url;
    private String XAuthToken;
    private String clientId;
}
