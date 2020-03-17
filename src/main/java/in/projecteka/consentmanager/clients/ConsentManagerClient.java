package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.dataflow.model.ConsentArtefactRepresentation;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

import static java.lang.String.format;
import static java.util.function.Predicate.not;

@AllArgsConstructor
public class ConsentManagerClient {
    private final WebClient.Builder webClientBuilder;
    private final String url;
    private final Supplier<Mono<String>> tokenGenerator;

    public Mono<ConsentArtefactRepresentation> getConsentArtefact(String consentArtefactId) {
        return tokenGenerator.get()
                .flatMap(token -> webClientBuilder.build()
                        .get()
                        .uri(format("%s/internal/consents/%s", url, consentArtefactId))
                        .header("Authorization", token)
                        .retrieve()
                        .onStatus(not(HttpStatus::is2xxSuccessful),
                                clientResponse -> Mono.error(ClientError.consentArtefactNotFound()))
                        .bodyToMono(ConsentArtefactRepresentation.class));
    }
}
