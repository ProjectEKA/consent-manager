package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.properties.GatewayServiceProperties;
import in.projecteka.consentmanager.dataflow.model.hip.DataFlowRequest;
import in.projecteka.consentmanager.dataflow.model.hip.DataRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

import static in.projecteka.consentmanager.clients.ClientError.unknownErrorOccurred;
import static in.projecteka.consentmanager.common.Constants.HDR_HIP_ID;
import static in.projecteka.consentmanager.common.Constants.V_1_HEALTH_INFORMATION_REQUEST_FORMAT;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@AllArgsConstructor
public class DataRequestNotifier {
    private final WebClient webClientBuilder;
    private final Supplier<Mono<String>> tokenGenerator;
    private final GatewayServiceProperties gatewayServiceProperties;

    public Mono<Void> notifyHip(DataFlowRequest dataFlowRequest, String hipUrl) {
        return tokenGenerator.get()
                .flatMap(token ->
                        webClientBuilder
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

    public Mono<Void> notifyHip(DataRequest dataFlowRequest, String hipId) {
        return tokenGenerator.get()
                .flatMap(token ->
                        webClientBuilder
                                .post()
                                .uri(getDataFlowRequestUrl())
                                .header(AUTHORIZATION, token)
                                .header(HDR_HIP_ID, hipId)
                                .bodyValue(dataFlowRequest)
                                .retrieve()
                                .onStatus(HttpStatus::is5xxServerError,
                                        clientResponse -> Mono.error(unknownErrorOccurred()))
                                .toBodilessEntity())
                .then();
    }

    private String getDataFlowRequestUrl() {
        return String.format(V_1_HEALTH_INFORMATION_REQUEST_FORMAT, gatewayServiceProperties.getBaseUrl());
    }
}
