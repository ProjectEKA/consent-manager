package in.org.projecteka.hdaf.link;

import in.org.projecteka.hdaf.link.link.FetchUserId;
import in.org.projecteka.hdaf.link.link.model.PatientLinkReferenceRequest;
import in.org.projecteka.hdaf.link.link.model.PatientLinkReferenceRequestHIP;
import in.org.projecteka.hdaf.link.link.model.PatientLinkReferenceResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class HIPClient {

    private final FetchUserId fetchUserId;
    private final WebClient.Builder webClientBuilder;

    public HIPClient(FetchUserId fetchUserId, WebClient.Builder webClientBuilder) {
        this.fetchUserId = fetchUserId;
        this.webClientBuilder = webClientBuilder;
    }

    public Flux<PatientLinkReferenceResponse> linkPatientCareContext(String authorization, PatientLinkReferenceRequest patientLinkReferenceRequest) {
        String userId = fetchUserId.fetchUserIdFromHeader(authorization);
        PatientLinkReferenceRequestHIP patientLinkReferenceRequestHIP = new PatientLinkReferenceRequestHIP(userId, patientLinkReferenceRequest.getPatient());
        return webClientBuilder.build()
                .post()
                .uri(String.format("http://localhost:8001/patients/link"))
                .body(Mono.just(patientLinkReferenceRequestHIP), PatientLinkReferenceRequestHIP.class)
                .retrieve()
                .bodyToFlux(PatientLinkReferenceResponse.class);
    }
}
