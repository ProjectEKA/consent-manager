package in.projecteka.dataflow;

import in.projecteka.dataflow.model.hip.DataRequest;
import in.projecteka.dataflow.properties.GatewayServiceProperties;
import lombok.AllArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

import static in.projecteka.dataflow.Constants.HDR_HIP_ID;
import static in.projecteka.dataflow.Constants.PATH_HEALTH_HIP_INFORMATION_REQUEST;
import static in.projecteka.library.clients.model.ClientError.unknownErrorOccurred;
import static in.projecteka.library.common.Constants.CORRELATION_ID;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@AllArgsConstructor
public class DataRequestNotifier {
    private final WebClient webClientBuilder;
    private final Supplier<Mono<String>> tokenGenerator;
    private final GatewayServiceProperties gatewayServiceProperties;

    public Mono<Void> notifyHip(DataRequest dataFlowRequest, String hipId) {
        return tokenGenerator.get()
                .flatMap(token ->
                        webClientBuilder
                                .post()
                                .uri(getDataFlowRequestUrl())
                                .header(AUTHORIZATION, token)
                                .header(HDR_HIP_ID, hipId)
                                .header(CORRELATION_ID, MDC.get(CORRELATION_ID))
                                .bodyValue(dataFlowRequest)
                                .retrieve()
                                .onStatus(HttpStatus::is5xxServerError,
                                        clientResponse -> Mono.error(unknownErrorOccurred()))
                                .toBodilessEntity())
                .then();
    }

    private String getDataFlowRequestUrl() {
        return gatewayServiceProperties.getBaseUrl() + PATH_HEALTH_HIP_INFORMATION_REQUEST;
    }
}
