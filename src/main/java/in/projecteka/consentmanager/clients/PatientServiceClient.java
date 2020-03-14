package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.model.LinkedCareContexts;
import in.projecteka.consentmanager.clients.properties.LinkServiceProperties;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

public class PatientServiceClient {

    private final WebClient.Builder webClientBuilder;
    private LinkServiceProperties serviceProperties;

    public PatientServiceClient(WebClient.Builder webClientBuilder, LinkServiceProperties serviceProperties) {
        this.webClientBuilder = webClientBuilder;
        this.serviceProperties = serviceProperties;
    }

    public Mono<LinkedCareContexts> retrievePatientLinks(String authorization) {
        return webClientBuilder.build()
                .get()
                .uri(String.format("%s/patients/links", serviceProperties.getUrl()))
                .header(AUTHORIZATION, authorization)
                .retrieve()
                .onStatus(httpStatus -> httpStatus.value() == 401,
                        clientResponse -> Mono.error(ClientError.unAuthorized()))
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .bodyToMono(LinkedCareContexts.class);
    }
}
