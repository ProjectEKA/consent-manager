package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.model.ErrorRepresentation;
import in.projecteka.consentmanager.clients.model.PatientLinkReferenceRequest;
import in.projecteka.consentmanager.clients.model.PatientLinkReferenceResponse;
import in.projecteka.consentmanager.clients.model.PatientLinkRequest;
import in.projecteka.consentmanager.clients.model.PatientLinkResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static java.util.function.Predicate.not;

public class LinkServiceClient {

    private final WebClient.Builder webClientBuilder;

    public LinkServiceClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public Mono<PatientLinkReferenceResponse> linkPatientEnquiry(
            PatientLinkReferenceRequest patientLinkReferenceRequest,
            String url) {
        return webClientBuilder.build()
                .post()
                .uri(String.format("%s/patients/link", url))
                .body(Mono.just(patientLinkReferenceRequest), PatientLinkReferenceRequest.class)
                .retrieve()
                .onStatus(not(HttpStatus::is2xxSuccessful), clientResponse ->
                        clientResponse.bodyToMono(ErrorRepresentation.class)
                                .flatMap(e -> Mono.error(new ClientError(clientResponse.statusCode(), e))))
                .bodyToMono(PatientLinkReferenceResponse.class);
    }

    public Mono<PatientLinkResponse> linkPatientConfirmation(
            String linkRefNumber,
            PatientLinkRequest patientLinkRequest,
            String url) {
        return webClientBuilder.build()
                .post()
                .uri(String.format("%s/patients/link/%s", url, linkRefNumber))
                .body(Mono.just(patientLinkRequest), PatientLinkRequest.class)
                .retrieve()
                .onStatus(not(HttpStatus::is2xxSuccessful), clientResponse ->
                        clientResponse.bodyToMono(ErrorRepresentation.class)
                                .flatMap(e -> Mono.error(new ClientError(clientResponse.statusCode(), e))))
                .bodyToMono(PatientLinkResponse.class);
    }
}
