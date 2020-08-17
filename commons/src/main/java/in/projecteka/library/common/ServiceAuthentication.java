package in.projecteka.library.common;

import in.projecteka.library.clients.ServiceAuthenticationClient;
import in.projecteka.library.common.cache.CacheAdapter;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class ServiceAuthentication {
    private final ServiceAuthenticationClient client;
    private final ServiceCredential properties;
    private final CacheAdapter<String, String> accessTokenCache;

    public Mono<String> authenticate() {
        return accessTokenCache.getIfPresent("consentManager:gateway:accessToken")
                .switchIfEmpty(Mono.defer(this::tokenUsingSecret))
                .map(token -> String.format("%s %s", "Bearer", token));
    }

    private Mono<String> tokenUsingSecret() {
        return client.getTokenFor(properties.getClientId(), properties.getClientSecret())
                .flatMap(session ->
                        accessTokenCache.put("consentManager:gateway:accessToken", session.getAccessToken())
                                .thenReturn(session.getAccessToken()));
    }
}
