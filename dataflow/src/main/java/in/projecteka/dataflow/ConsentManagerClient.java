package in.projecteka.dataflow;

import in.projecteka.dataflow.model.ConsentArtefactRepresentation;
import in.projecteka.dataflow.model.ConsentArtefactsStatusResponse;
import in.projecteka.library.clients.model.ClientError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Properties;
import java.util.function.Supplier;

import static java.lang.String.format;
import static java.util.function.Predicate.not;
import static reactor.core.publisher.Mono.error;

public class ConsentManagerClient {
    private static final Logger logger = LoggerFactory.getLogger(ConsentManagerClient.class);
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
                                clientResponse -> clientResponse.bodyToMono(Properties.class)
                                        .doOnNext(properties -> logger.error("Error: {}", properties))
                                        .then(error(ClientError.consentArtefactNotFound())))
                        .bodyToMono(ConsentArtefactRepresentation.class));
    }

    public Mono<ConsentArtefactsStatusResponse> getConsentArtefactStatus(String consentId) {
        return tokenGenerator.get()
                .flatMap(token -> webClient
                        .get()
                        .uri(format("%s/internal/consent-artefacts/status/%s", url, consentId))
                        .header("Authorization", token)
                        .retrieve()
                        .onStatus(HttpStatus::isError,
                                clientResponse -> clientResponse.bodyToMono(Properties.class)
                                        .doOnNext(properties -> logger.error("Error: {}", properties))
                                        .then(error(ClientError.consentArtefactNotFound())))
                        .bodyToMono(ConsentArtefactsStatusResponse.class));
    }
}