package in.org.projecteka.hdaf.clients;

import in.org.projecteka.hdaf.clients.properties.ClientRegistryProperties;
import in.org.projecteka.hdaf.link.discovery.model.Provider;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ClientRegistryClient {

    private final WebClient.Builder webClientBuilder;
    private final ClientRegistryProperties clientRegistryProperties;

    public ClientRegistryClient(
            WebClient.Builder webClientBuilder,
            ClientRegistryProperties clientRegistryProperties) {
        this.webClientBuilder = webClientBuilder;
        this.clientRegistryProperties = clientRegistryProperties;
    }

    public Flux<Provider> providersOf(String name) {
        return webClientBuilder.build()
                .get()
                .uri(String.format("%s/providers?name=%s", clientRegistryProperties.getUrl(), name))
                .header("client_id", clientRegistryProperties.getClientId())
                .header("X-Auth-Token", clientRegistryProperties.getXAuthToken())
                .retrieve()
                .bodyToFlux(Provider.class);
    }

    public Mono<Provider> providerWith(String id) {
        return webClientBuilder.build()
                .get()
                .uri(String.format("%s/providers/%s", clientRegistryProperties.getUrl(), id))
                .header("client_id", clientRegistryProperties.getClientId())
                .header("X-Auth-Token", clientRegistryProperties.getXAuthToken())
                .retrieve()
                .bodyToMono(Provider.class);
    }
}
