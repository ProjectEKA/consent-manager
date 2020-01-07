package in.org.projecteka.hdaf.link;

import in.org.projecteka.hdaf.link.link.model.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class HIPClient {

    private final WebClient.Builder webClientBuilder;

    public HIPClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public Flux<PatientLinkReferenceResponse> linkPatientCareContext(String patientId, PatientLinkReferenceRequest patientLinkReferenceRequest, String url) {
        PatientLinkReferenceRequestHIP patientLinkReferenceRequestHIP = new PatientLinkReferenceRequestHIP(patientId, patientLinkReferenceRequest.getPatient());
        return webClientBuilder.build()
                .post()
                .uri(String.format("%s/patients/link", url))
                .body(Mono.just(patientLinkReferenceRequestHIP), PatientLinkReferenceRequestHIP.class)
                .retrieve()
                .bodyToFlux(PatientLinkReferenceResponse.class);
    }

    public Flux<PatientLinkResponse> validateToken(String patientId, String linkRefNumber, PatientLinkRequest patientLinkRequest, String url) {
        return webClientBuilder.build()
                .post()
                .uri(String.format("%s/patients/link/%s", url, linkRefNumber))
                .body(Mono.just(patientLinkRequest), PatientLinkRequest.class)
                .retrieve()
                .bodyToFlux(PatientLinkResponse.class);
    }
}
