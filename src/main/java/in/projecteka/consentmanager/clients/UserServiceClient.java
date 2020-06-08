package in.projecteka.consentmanager.clients;

import com.google.common.net.HttpHeaders;
import in.projecteka.consentmanager.clients.model.User;
import in.projecteka.consentmanager.clients.properties.GatewayServiceProperties;
import in.projecteka.consentmanager.user.model.PatientResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Supplier;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@AllArgsConstructor
public class UserServiceClient {

    private final WebClient.Builder webClientBuilder;
    private final String url;
    private final Supplier<Mono<String>> tokenGenerator;
    private static final String PATIENT_FIND_URL_PATH = "/patients/on-find";
    private final GatewayServiceProperties gatewayServiceProperties;

    public Mono<User> userOf(String userId) {
        return tokenGenerator.get().flatMap(token ->
                webClientBuilder.build()
                        .get()
                        .uri(String.format("%s/internal/users/%s/", url, userId))
                        .header(AUTHORIZATION, token)
                        .retrieve()
                        .onStatus(httpStatus -> httpStatus.value() == 404,
                                clientResponse -> Mono.error(ClientError.userNotFound()))
                        .bodyToMono(User.class));
    }

    public Mono<Void> sendPatientResponseToGateWay(PatientResponse patientResponse, String x_requesterId, String requesterId) {
        return tokenGenerator.get()
                .flatMap(token ->
                        webClientBuilder.build()
                                .post()
                                .uri(gatewayServiceProperties.getBaseUrl() + PATIENT_FIND_URL_PATH)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, token)
                                .header(x_requesterId, requesterId)
                                .bodyValue(patientResponse)
                                .retrieve()
                                .onStatus(httpStatus -> httpStatus.value() == 401,
                                        clientResponse -> Mono.error(ClientError.unAuthorized()))
                                .onStatus(HttpStatus::is5xxServerError,
                                        clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                                .toBodilessEntity()
                                .timeout(Duration.ofMillis(gatewayServiceProperties.getRequestTimeout()))
                ).then();
    }
}
