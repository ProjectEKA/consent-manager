package in.projecteka.hdaf.clients;

import in.projecteka.hdaf.link.discovery.model.patient.request.PatientRequest;
import in.projecteka.hdaf.link.discovery.model.patient.response.PatientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class HipServiceClient {

    private final WebClient.Builder webClientBuilder;

    public HipServiceClient(
            WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public Mono<PatientResponse> patientFor(PatientRequest request, String url) {
        return webClientBuilder.build()
                .post()
                .uri(url + "/patients/discover/")
                .bodyValue(request)
                .retrieve()
                .onStatus(httpStatus -> httpStatus.value() == 404, clientResponse -> Mono.error(new Throwable("Hip returned 404")))
                .bodyToMono(PatientResponse.class);
    }
}