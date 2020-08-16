package in.projecteka.library.common;

import in.projecteka.library.clients.ClientRegistryClient;
import in.projecteka.library.clients.model.Provider;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class CentralRegistry {
    private final ClientRegistryClient clientRegistryClient;

    public Mono<Provider> providerWith(String providerId) {
        return clientRegistryClient.providerWith(providerId);
    }

    public Flux<Provider> providersOf(String providerName) {
        return clientRegistryClient.providersOf(providerName);
    }
}