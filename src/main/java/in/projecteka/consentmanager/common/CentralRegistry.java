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
        var accessToken = accessTokenCache.getIfPresent("accessToken").switchIfEmpty(Mono.just(""));
        return accessToken.flatMap(token -> {
            if (token.isEmpty()) {
                return clientRegistryClient.getTokenFor(properties.getClientId(), properties.getXAuthToken())
                        .flatMap(session -> accessTokenCache.put("accessToken", session.getAccessToken())
                                .then(Mono.just(String.format("%s %s", session.getTokenType(), session.getAccessToken()))));
            }
            return Mono.just(String.format("%s %s", "Bearer", token));
        });
    }
}


