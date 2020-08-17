package in.projecteka.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "user.gatewayservice")
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

