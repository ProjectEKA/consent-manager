package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.model.LinkedCareContexts;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@AllArgsConstructor
public class PatientServiceClient {

    private final WebClient.Builder webClientBuilder;
    private final Supplier<Mono<String>> tokenGenerator;
    private final String baseUrl;

    public Mono<LinkedCareContexts> retrievePatientLinks(String username) {
        return tokenGenerator.get()
                .flatMap(authorization ->
                        webClientBuilder.build()
                                .get()
                                .uri(String.format("%s/internal/patients/%s/links", baseUrl, username))
                                .header(AUTHORIZATION, authorization)
                                .retrieve()
                                .onStatus(httpStatus -> httpStatus.value() == 401,
                                        clientResponse -> Mono.error(ClientError.unAuthorized()))
                                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                                .bodyToMono(LinkedCareContexts.class));
    }
}
