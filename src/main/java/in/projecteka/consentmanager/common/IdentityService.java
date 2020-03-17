package in.projecteka.consentmanager.common;

import in.projecteka.consentmanager.clients.IdentityServiceClient;
import in.projecteka.consentmanager.clients.properties.IdentityServiceProperties;
import lombok.AllArgsConstructor;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

import static java.lang.String.format;

@AllArgsConstructor
public class IdentityService {
    private final IdentityServiceClient identityServiceClient;
    private final IdentityServiceProperties identityServiceProperties;

    public Mono<String> authenticate() {
        return identityServiceClient.getToken(loginRequestWith())
                .map(session -> format("%s %s", session.getTokenType(), session.getAccessToken()));
    }

    private MultiValueMap<String, String> loginRequestWith() {
        var formData = new LinkedMultiValueMap<String, String>();
        formData.add("grant_type", "client_credentials");
        formData.add("scope", "openid");
        formData.add("client_id", identityServiceProperties.getClientId());
        formData.add("client_secret", identityServiceProperties.getClientSecret());
        return formData;
    }
}