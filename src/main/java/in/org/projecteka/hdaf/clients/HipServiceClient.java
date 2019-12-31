package in.org.projecteka.hdaf.clients;

import in.org.projecteka.hdaf.clients.properties.HipServiceProperties;
import in.org.projecteka.hdaf.link.discovery.model.patient.PatientRequest;
import in.org.projecteka.hdaf.link.discovery.model.patient.Patient;
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

    public Mono<Patient> patientFor(PatientRequest request, String url) {
        return webClientBuilder.build()
                .post()
                .uri(url)
                .header("X-Auth-Token", hipServiceProperties.getXAuthToken())
                .body(request, PatientRequest.class)
                .retrieve()
                .bodyToMono(Patient.class);
    }
}
