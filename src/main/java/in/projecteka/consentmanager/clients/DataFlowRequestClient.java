package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.properties.GatewayServiceProperties;
import in.projecteka.consentmanager.common.CentralRegistry;
import in.projecteka.consentmanager.dataflow.model.DataFlowRequestResult;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static in.projecteka.consentmanager.common.Constants.HDR_HIU_ID;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@AllArgsConstructor
public class DataFlowRequestClient {
    private static final String DATA_FLOW_REQUEST_URL_PATH = "/health-information/on-request";
    private final WebClient.Builder webClientBuilder;
    private final GatewayServiceProperties gatewayServiceProperties;
    private final CentralRegistry centralRegistry;

    public Mono<Void> sendHealthInformationResponseToGateway(DataFlowRequestResult dataFlowRequest, String hiuId) {
        return centralRegistry.authenticate()
                .flatMap(authToken ->
                        webClientBuilder.build()
                                .post()
                                .uri(gatewayServiceProperties.getBaseUrl() + DATA_FLOW_REQUEST_URL_PATH)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(AUTHORIZATION, authToken)
                                .header(HDR_HIU_ID, hiuId)
                                .bodyValue(dataFlowRequest)
                                .retrieve()
                                .onStatus(httpStatus -> httpStatus.value() == 401,
                                        clientResponse -> Mono.error(ClientError.unAuthorized()))
                                .onStatus(HttpStatus::is5xxServerError,
                                        clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                                .toBodilessEntity()
                                .timeout(Duration.ofMillis(gatewayServiceProperties.getRequestTimeout())))
                .then();
    }
}
