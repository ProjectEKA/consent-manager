package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.properties.GatewayServiceProperties;
import in.projecteka.consentmanager.link.discovery.model.patient.request.PatientRequest;
import in.projecteka.consentmanager.link.discovery.model.patient.response.PatientResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Supplier;

import static in.projecteka.consentmanager.clients.HeaderConstants.HDR_HIP_ID;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@AllArgsConstructor
public class DiscoveryServiceClient {

    private static final String PATIENTS_CARE_CONTEXTS_DISCOVERY_URL_PATH = "/care-contexts/discover";
    private final WebClient.Builder webClientBuilder;
    private final Supplier<Mono<String>> tokenGenerator;
    private final GatewayServiceProperties gatewayServiceProperties;

    @Deprecated
    public Mono<PatientResponse> patientFor(PatientRequest request, String url, String hipId) {
        return tokenGenerator.get()
                .map(token ->
                        webClientBuilder.build()
                                .post()
                                .uri(url + "/patients/discover/carecontexts")
                                .header(AUTHORIZATION, token)
                                .header(HDR_HIP_ID, hipId)
                                .bodyValue(request)
                                .retrieve())
                .map(responseSpec -> responseSpec
                        .onStatus(httpStatus -> httpStatus.value() == 404,
                                clientResponse -> Mono.error(ClientError.userNotFound()))
                        .onStatus(HttpStatus::is5xxServerError,
                                clientResponse -> Mono.error(ClientError.networkServiceCallFailed())))
                .flatMap(responseSpec -> responseSpec.bodyToMono(PatientResponse.class));
    }

    public Mono<Boolean> requestPatientFor(PatientRequest request, String hipId) {
        return tokenGenerator.get()
                .flatMap(token ->
                        webClientBuilder.build()
                                .post()
                                .uri(gatewayServiceProperties.getBaseUrl() + PATIENTS_CARE_CONTEXTS_DISCOVERY_URL_PATH)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(AUTHORIZATION, token)
                                .header(HDR_HIP_ID, hipId)
                                .bodyValue(request)
                                .retrieve()
                                .onStatus(httpStatus -> httpStatus.value() == 401,
                                        // Error msg should be logged
                                        clientResponse -> Mono.error(ClientError.unknownErrorOccurred()))
                                .onStatus(httpStatus -> httpStatus.value() == 404,
                                        clientResponse -> Mono.error(ClientError.userNotFound()))
                                .onStatus(HttpStatus::is5xxServerError,
                                        clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                                .toBodilessEntity()
                                .timeout(Duration.ofMillis(gatewayServiceProperties.getRequestTimeout()))
                ).thenReturn(Boolean.TRUE);
    }
}