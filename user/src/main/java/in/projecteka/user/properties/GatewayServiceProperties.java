package in.projecteka.user.properties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "user.gateway-service")
@Getter
@AllArgsConstructor
@ConstructorBinding
public class GatewayServiceProperties {
    private final String baseUrl;
    private final int requestTimeout;
    private final String clientSecret;
    private final String clientId;
    private final String jwkUrl;
}

