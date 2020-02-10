package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.common.TokenUtils;
import in.projecteka.consentmanager.dataflow.DataFlowAuthServerProperties;
import in.projecteka.consentmanager.dataflow.DataFlowConsentManagerProperties;
import in.projecteka.consentmanager.dataflow.model.ConsentArtefactRepresentation;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static java.util.function.Predicate.not;

@AllArgsConstructor
public class ConsentManagerClient {
    private final WebClient.Builder webClientBuilder;
    private DataFlowAuthServerProperties dataFlowAuthServerProperties;
    private DataFlowConsentManagerProperties dataFlowConsentManagerProperties;

    public Mono<ConsentArtefactRepresentation> getConsentArtifact(String consentArtefactId) {
        return webClientBuilder.build()
                .get()
                .uri(String.format("%s/internal/consents/%s", dataFlowConsentManagerProperties.getUrl(), consentArtefactId))
                .header("Authorization", TokenUtils.encode(dataFlowAuthServerProperties.getClientId()))
                .retrieve()
                .onStatus(not(HttpStatus::is2xxSuccessful),
                        clientResponse -> Mono.error(ClientError.consentArtefactNotFound()))
                .bodyToMono(ConsentArtefactRepresentation.class);
    }
}
