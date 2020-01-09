package in.org.projecteka.hdaf.clients;

import in.org.projecteka.hdaf.link.discovery.model.patient.request.PatientRequest;
import in.org.projecteka.hdaf.link.discovery.model.patient.response.HipPatientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class HipServiceClient {

    private final WebClient.Builder webClientBuilder;

    public HipServiceClient(
            WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public Mono<HipPatientResponse> patientFor(PatientRequest request, String url) {
        return webClientBuilder.build()
                .post()
                .uri(url + "/patients/discover/")
                .bodyValue(request)
                .retrieve()
                .onStatus(httpStatus -> httpStatus.value() == 404, clientResponse -> Mono.error(new Throwable("Hip returned 404")))
                .bodyToMono(HipPatientResponse.class);
    }
}