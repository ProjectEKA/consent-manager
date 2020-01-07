package in.org.projecteka.hdaf.clients;

import in.org.projecteka.hdaf.clients.properties.HipServiceProperties;
import in.org.projecteka.hdaf.link.discovery.model.patient.request.PatientRequest;
import in.org.projecteka.hdaf.link.discovery.model.patient.response.PatientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class HipServiceClient {

    private final WebClient.Builder webClientBuilder;
    private HipServiceProperties hipServiceProperties;

    public HipServiceClient(
            WebClient.Builder webClientBuilder,
            HipServiceProperties hipServiceProperties) {
        this.webClientBuilder = webClientBuilder;
        this.hipServiceProperties = hipServiceProperties;
    }

    public Mono<PatientResponse> patientFor(PatientRequest request, String url) {
        return webClientBuilder.build()
                .post()
                .uri(url + "/patients/discover/")
                .header("X-Auth-Token", hipServiceProperties.getXAuthToken())
                .bodyValue(request)
                .retrieve()
                .onStatus(httpStatus -> httpStatus.value() == 404, clientResponse -> Mono.error(new Throwable("Hip returned 404")))
                .bodyToMono(PatientResponse.class);
    }
}