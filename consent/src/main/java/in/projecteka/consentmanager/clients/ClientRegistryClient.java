package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.model.Provider;
import in.projecteka.library.clients.model.ClientError;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static java.lang.String.format;

public class ClientRegistryClient {

    private static final String FR_PROVIDER_URL = "/api/2.0/organizations";
    private final WebClient webClient;

    public ClientRegistryClient(WebClient.Builder webClient, String baseUrl) {
        this.webClient = webClient.baseUrl(baseUrl).build();
    }

    public Flux<Provider> providersOf(String name) {
        return webClient
                .get()
                .uri(format(FR_PROVIDER_URL + "?name=%s", name))
                .retrieve()
                .bodyToFlux(Provider.class);
    }

    public Mono<Provider> providerWith(String id) {
        return webClient
                .get()
                .uri(format(FR_PROVIDER_URL + "/%s", id))
                .retrieve()
                .onStatus(httpStatus -> httpStatus.value() == 404,
                        clientResponse -> Mono.error(ClientError.providerNotFound()))
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .bodyToMono(Provider.class);
    }
}
