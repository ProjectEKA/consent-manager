package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.common.TokenUtils;
import in.projecteka.consentmanager.dataflow.DataFlowServiceProperties;
import in.projecteka.consentmanager.dataflow.model.ConsentArtefactRepresentation;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static java.util.function.Predicate.not;

@AllArgsConstructor
public class ConsentManagerClient {
    private final WebClient.Builder webClientBuilder;
    private DataFlowServiceProperties dataFlowServiceProperties;

    public Mono<ConsentArtefactRepresentation> getConsentArtifact(String consentArtefactId) {
        return webClientBuilder.build()
                .get()
                .uri(String.format("%s/internal/consents/%s", dataFlowServiceProperties.getUrl(), consentArtefactId))
                .header("Authorization", TokenUtils.encode(dataFlowServiceProperties.getClientId()))
                .retrieve()
                .onStatus(not(HttpStatus::is2xxSuccessful),
                        clientResponse -> Mono.error(ClientError.consentArtefactNotFound()))
                .bodyToMono(ConsentArtefactRepresentation.class);
    }
}
