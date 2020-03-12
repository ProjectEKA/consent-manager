package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.link.discovery.model.patient.request.PatientRequest;
import in.projecteka.consentmanager.link.discovery.model.patient.response.PatientResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

public class DiscoveryServiceClient {

    private final WebClient.Builder webClientBuilder;

    public DiscoveryServiceClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public Mono<PatientResponse> patientFor(PatientRequest request, String url, String authorization) {
        return webClientBuilder.build()
                .post()
                .uri(url + "/patients/discover/")
                .header(AUTHORIZATION, authorization)
                .bodyValue(request)
                .retrieve()
                .onStatus(httpStatus -> httpStatus.value() == 404,
                        clientResponse -> Mono.error(ClientError.userNotFound()))
                .onStatus(HttpStatus::is5xxServerError,
                        clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .bodyToMono(PatientResponse.class);
    }
}