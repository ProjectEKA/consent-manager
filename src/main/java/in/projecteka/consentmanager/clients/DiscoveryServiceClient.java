package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.link.discovery.model.patient.request.PatientRequest;
import in.projecteka.consentmanager.link.discovery.model.patient.response.PatientResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@AllArgsConstructor
public class DiscoveryServiceClient {

    private static final String PATIENTS_CARE_CONTEXTS_DISCOVERY_URL_PATH = "/patients/care-contexts/discover";
    private final WebClient.Builder webClientBuilder;
    private final Supplier<Mono<String>> tokenGenerator;

    public Mono<PatientResponse> patientFor(PatientRequest request, String url, String hipId) {
        return tokenGenerator.get()
                .map(token ->
                        webClientBuilder.build()
                                .post()
                                .uri(url + "/patients/discover/carecontexts")
                                .header(AUTHORIZATION, token)
                                .header("X-HIP-ID", hipId)
                                .bodyValue(request)
                                .retrieve())
                .map(responseSpec -> responseSpec
                        .onStatus(httpStatus -> httpStatus.value() == 404,
                                clientResponse -> Mono.error(ClientError.userNotFound()))
                        .onStatus(HttpStatus::is5xxServerError,
                                clientResponse -> Mono.error(ClientError.networkServiceCallFailed())))
                .flatMap(responseSpec -> responseSpec.bodyToMono(PatientResponse.class));
    }

    public Mono<PatientResponse> requestPatientFor(PatientRequest request, String url, String hipId) {
        return tokenGenerator.get()
                .map(token ->
                        webClientBuilder.build()
                                .post()
                                .uri(url + PATIENTS_CARE_CONTEXTS_DISCOVERY_URL_PATH)
                                .header(AUTHORIZATION, token)
                                .header("X-HIP-ID", hipId)
                                .bodyValue(request)
                                .retrieve())
                .map(responseSpec -> responseSpec
                        .onStatus(httpStatus -> httpStatus.value() == 404,
                                clientResponse -> Mono.error(ClientError.userNotFound()))
                        .onStatus(HttpStatus::is5xxServerError,
                                clientResponse -> Mono.error(ClientError.networkServiceCallFailed())))
                .flatMap(responseSpec -> responseSpec.bodyToMono(PatientResponse.class));
    }

//    public Mono<Void> requestPatientFor(PatientRequest request, String url, String hipId) {
//        return tokenGenerator.get()
//                .flatMap(token ->
//                        webClientBuilder.build()
//                                .post()
//                                .uri(url + PATIENTS_CARE_CONTEXTS_DISCOVERY_URL_PATH)
//                                .header(AUTHORIZATION, token)
//                                .header("X-HIP-ID", hipId)
//                                .bodyValue(request)
//                                .retrieve()
//                                .onStatus(httpStatus -> httpStatus.value() == 404,
//                                        clientResponse -> Mono.error(ClientError.userNotFound()))
//                                .onStatus(HttpStatus::is5xxServerError,
//                                        clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
//                                .toBodilessEntity())
//                                .then();
//
//    }
}