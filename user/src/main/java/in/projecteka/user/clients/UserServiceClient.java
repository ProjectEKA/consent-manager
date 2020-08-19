package in.projecteka.user.clients;

import in.projecteka.library.clients.model.ClientError;
import in.projecteka.library.common.ServiceAuthentication;
import in.projecteka.user.properties.GatewayServiceProperties;
import in.projecteka.user.model.PatientResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static reactor.core.publisher.Mono.error;

@AllArgsConstructor
public class UserServiceClient {

    private static final String PATIENT_FIND_URL_PATH = "/patients/on-find";
    private final WebClient webClient;
    private final GatewayServiceProperties gatewayServiceProperties;
    private final ServiceAuthentication serviceAuthentication;


    public Mono<Void> sendPatientResponseToGateWay(PatientResponse patientResponse,
                                                   String routingKey,
                                                   String requesterId) {
        return serviceAuthentication.authenticate()
                .flatMap(authToken ->
                        webClient
                                .post()
                                .uri(gatewayServiceProperties.getBaseUrl() + PATIENT_FIND_URL_PATH)
                                .contentType(APPLICATION_JSON)
                                .header(AUTHORIZATION, authToken)
                                .header(routingKey, requesterId)
                                .bodyValue(patientResponse)
                                .retrieve()
                                .onStatus(httpStatus -> httpStatus.value() == 401,
                                        clientResponse -> error(ClientError.unAuthorized()))
                                .onStatus(HttpStatus::is5xxServerError,
                                        clientResponse -> error(ClientError.networkServiceCallFailed()))
                                .toBodilessEntity()
                                .timeout(Duration.ofMillis(gatewayServiceProperties.getRequestTimeout())))
                .then();
    }
}

