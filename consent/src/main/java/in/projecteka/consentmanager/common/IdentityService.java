package in.projecteka.consentmanager.common;

import in.projecteka.consentmanager.clients.IdentityServiceClient;
import in.projecteka.consentmanager.clients.properties.IdentityServiceProperties;
import in.projecteka.library.common.cache.CacheAdapter;
import lombok.AllArgsConstructor;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class IdentityService {
    private final IdentityServiceClient identityServiceClient;
    private final IdentityServiceProperties identityServiceProperties;
    private final CacheAdapter<String, String> accessTokenCache;

    public Mono<String> authenticate() {
        return accessTokenCache.getIfPresent("consentManager:accessToken")
                .switchIfEmpty(Mono.defer(this::tokenUsingSecret))
                .map(token -> String.format("%s %s", "Bearer", token));
    }

    private Mono<String> tokenUsingSecret() {
        return identityServiceClient.getToken(loginRequestWith())
                .flatMap(session ->
                        accessTokenCache.put("consentManager:accessToken", session.getAccessToken())
                                .thenReturn(session.getAccessToken()));
    }

    private MultiValueMap<String, String> loginRequestWith() {
        var formData = new LinkedMultiValueMap<String, String>();
        formData.add("grant_type", "client_credentials");
        formData.add("scope", "openid");
        formData.add("client_id", identityServiceProperties.getClientId());
        formData.add("client_secret", identityServiceProperties.getClientSecret());
        return formData;
    }

    public String getConsentManagerId() {
        return identityServiceProperties.getClientId();
    }
}