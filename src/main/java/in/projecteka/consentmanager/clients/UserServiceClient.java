package in.projecteka.consentmanager.clients;

import com.google.common.net.HttpHeaders;
import in.projecteka.consentmanager.clients.model.User;
import in.projecteka.consentmanager.clients.properties.GatewayServiceProperties;
import in.projecteka.consentmanager.common.ServiceAuthentication;
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

    private static final String PATIENT_FIND_URL_PATH = "/patients/on-find";
    private static final String INTERNAL_PATH_USER_IDENTIFICATION = "%s/internal/users/%s/";
    private final WebClient webClient;
    private final String url;
    private final Supplier<Mono<String>> tokenGenerator;
    private final GatewayServiceProperties gatewayServiceProperties;
    private final ServiceAuthentication serviceAuthentication;

    public Mono<User> userOf(String userId) {
        return tokenGenerator.get()
                .flatMap(token ->
                        webClient
                                .get()
                                .uri(String.format(INTERNAL_PATH_USER_IDENTIFICATION, url, userId))
                                .header(AUTHORIZATION, token)
                                .retrieve()
                                .onStatus(httpStatus -> httpStatus.value() == 404,
                                        clientResponse -> Mono.error(ClientError.userNotFound()))
                                .bodyToMono(User.class));
    }

    public Mono<Void> sendPatientResponseToGateWay(PatientResponse patientResponse,
                                                   String routingKey,
                                                   String requesterId) {
        return serviceAuthentication.authenticate()
                .flatMap(authToken ->
                        webClient
                                .post()
                                .uri(gatewayServiceProperties.getBaseUrl() + PATIENT_FIND_URL_PATH)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, authToken)
                                .header(routingKey, requesterId)
                                .bodyValue(patientResponse)
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
