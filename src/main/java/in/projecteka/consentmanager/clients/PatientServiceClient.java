package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.model.LinkedCareContexts;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

@AllArgsConstructor
public class PatientServiceClient {

    private static final String INTERNAL_PATH_PATIENT_LINKS = "%s/internal/patients/%s/links";
    private final WebClient webClientBuilder;
    private final Supplier<Mono<String>> tokenGenerator;
    private final String baseUrl;
    private final String authorizationHeader;

    public Mono<LinkedCareContexts> retrievePatientLinks(String username) {
        return tokenGenerator.get()
                .flatMap(authorization ->
                        webClientBuilder
                                .get()
                                .uri(String.format(INTERNAL_PATH_PATIENT_LINKS, baseUrl, username))
                                .header(authorizationHeader, authorization)
                                .retrieve()
                                .onStatus(httpStatus -> httpStatus.value() == 401,
                                        clientResponse -> Mono.error(ClientError.unAuthorized()))
                                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                                .bodyToMono(LinkedCareContexts.class));
    }
}
