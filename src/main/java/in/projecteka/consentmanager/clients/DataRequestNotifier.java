package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.dataflow.model.hip.DataFlowRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static in.projecteka.consentmanager.clients.ClientError.unknownErrorOccurred;

@AllArgsConstructor
public class DataRequestNotifier {
    private WebClient.Builder webClientBuilder;

    public Mono<Void> notifyHip(DataFlowRequest dataFlowRequest, String hipUrl) {
        return webClientBuilder.build()
                .post()
                .uri(hipUrl + "/health-information/request")
                .bodyValue(dataFlowRequest)
                .retrieve()
                .onStatus(HttpStatus::is5xxServerError, clientResponse -> Mono.error(unknownErrorOccurred()))
                .toBodilessEntity()
                .then();
    }
}
