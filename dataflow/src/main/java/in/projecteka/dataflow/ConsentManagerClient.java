package in.projecteka.dataflow;

import in.projecteka.dataflow.model.ConsentArtefactRepresentation;
import in.projecteka.library.clients.model.ClientError;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

import static java.lang.String.format;
import static java.util.function.Predicate.not;

public class ConsentManagerClient {
    private final WebClient webClient;
    private final String url;

    public ConsentManagerClient(WebClient.Builder webClient,
                                String url, Supplier<Mono<String>> tokenGenerator) {
        this.webClient = webClient.build();
        this.url = url;
        this.tokenGenerator = tokenGenerator;
    }

    private final Supplier<Mono<String>> tokenGenerator;

    public Mono<ConsentArtefactRepresentation> getConsentArtefact(String consentArtefactId) {
        return tokenGenerator.get()
                .flatMap(token -> webClient
                        .get()
                        .uri(format("%s/internal/consents/%s", url, consentArtefactId))
                        .header("Authorization", token)
                        .retrieve()
                        .onStatus(not(HttpStatus::is2xxSuccessful),
                                clientResponse -> Mono.error(ClientError.consentArtefactNotFound()))
                        .bodyToMono(ConsentArtefactRepresentation.class));
    }
}