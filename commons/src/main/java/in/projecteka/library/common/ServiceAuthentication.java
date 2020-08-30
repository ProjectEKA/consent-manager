package in.projecteka.library.common;

import in.projecteka.library.clients.ServiceAuthenticationClient;
import in.projecteka.library.common.cache.CacheAdapter;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

import static java.lang.String.format;

@AllArgsConstructor
public class ServiceAuthentication {
    private final ServiceAuthenticationClient client;
    private final ServiceCredential properties;
    private final CacheAdapter<String, String> accessTokenCache;
    private final String prefix;

    public Mono<String> authenticate() {
        return accessTokenCache.getIfPresent(getKey())
                .switchIfEmpty(Mono.defer(this::tokenUsingSecret))
                .map(token -> format("%s %s", "Bearer", token));
    }

    private String getKey() {
        return format("%s:gateway:accessToken", prefix);
    }

    private Mono<String> tokenUsingSecret() {
        return client.getTokenFor(properties.getClientId(), properties.getClientSecret())
                .flatMap(session -> accessTokenCache.put(getKey(), session.getAccessToken())
                        .thenReturn(session.getAccessToken()));
    }
}
