package in.projecteka.consentmanager.common;

import in.projecteka.consentmanager.clients.ClientRegistryClient;
import in.projecteka.consentmanager.clients.model.Provider;
import in.projecteka.consentmanager.clients.properties.ClientRegistryProperties;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class CentralRegistry {
    private final ClientRegistryClient clientRegistryClient;
    private final ClientRegistryProperties properties;
    private final CacheAdapter<String, String> accessTokenCache;

    public Mono<Provider> providerWith(String providerId) {
        return authenticate()
                .flatMap(token -> clientRegistryClient.providerWith(providerId, token));
    }

    public Flux<Provider> providersOf(String providerName) {
        return authenticate()
                .flatMapMany(token -> clientRegistryClient.providersOf(providerName, token));
    }

    public Mono<String> authenticate() {
        return accessTokenCache.getIfPresent("consentManager:clientRegistry:accessToken")
                .switchIfEmpty(Mono.defer(this::tokenUsingSecret))
                .map(token -> String.format("%s %s", "Bearer", token));
    }

    private Mono<String> tokenUsingSecret() {
        return clientRegistryClient.getTokenFor(properties.getClientId(), properties.getXAuthToken())
                .flatMap(session -> accessTokenCache.put("consentManager:clientRegistry:accessToken", session.getAccessToken())
                        .thenReturn(session.getAccessToken()));
    }
}