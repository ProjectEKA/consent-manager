package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.dataflow.model.hip.DataFlowRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

import static in.projecteka.consentmanager.clients.ClientError.unknownErrorOccurred;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@AllArgsConstructor
public class DataRequestNotifier {
    private final WebClient.Builder webClientBuilder;
    private final Supplier<Mono<String>> tokenGenerator;

    public Mono<Void> notifyHip(DataFlowRequest dataFlowRequest, String hipUrl) {
        return tokenGenerator.get()
                .flatMap(token ->
                        webClientBuilder.build()
                                .post()
                                .uri(hipUrl + "/health-information/request")
                                .header(AUTHORIZATION, token)
                                .bodyValue(dataFlowRequest)
                                .retrieve()
                                .onStatus(HttpStatus::is5xxServerError,
                                        clientResponse -> Mono.error(unknownErrorOccurred()))
                                .toBodilessEntity())
                .then();
    }
}
