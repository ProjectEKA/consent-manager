package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.model.Provider;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static java.lang.String.format;

public class ClientRegistryClient {

    private final WebClient.Builder webClientBuilder;

    public ClientRegistryClient(WebClient.Builder webClientBuilder, String baseUrl) {
        this.webClientBuilder = webClientBuilder.baseUrl(baseUrl);
    }

    public Flux<Provider> providersOf(String name) {
        return webClientBuilder.build()
                .get()
                .uri(format("/api/2.0/providers?name=%s", name))
                .retrieve()
                .bodyToFlux(Provider.class);
    }

    public Mono<Provider> providerWith(String id) {
        return webClientBuilder.build()
                .get()
                .uri(format("/api/2.0/providers/%s", id))
                .retrieve()
                .onStatus(httpStatus -> httpStatus.value() == 404,
                        clientResponse -> Mono.error(ClientError.providerNotFound()))
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .bodyToMono(Provider.class);
    }
}
