package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.user.model.HASSignupRequest;
import in.projecteka.consentmanager.user.model.HealthAccountUser;
import in.projecteka.consentmanager.user.model.UpdateHASUserRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static in.projecteka.consentmanager.user.Constants.HAS_ACCOUNT_UPDATE;
import static in.projecteka.consentmanager.user.Constants.HAS_CREATE_ACCOUNT_VERIFIED_MOBILE_TOKEN;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class HASSignupServiceClient {

    private final WebClient webClient;

    public HASSignupServiceClient(WebClient.Builder webClient, String baseUrl) {
        this.webClient = webClient.baseUrl(baseUrl).build();
    }

    public Mono<HealthAccountUser> createHASAccount(HASSignupRequest request) {
        return webClient
                .post()
                .uri(uriBuilder -> uriBuilder.path(HAS_CREATE_ACCOUNT_VERIFIED_MOBILE_TOKEN).build())
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .body(Mono.just(request), HASSignupRequest.class)
                .accept(APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, clientResponse -> Mono.error(ClientError.unAuthorized()))
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .bodyToMono(HealthAccountUser.class);
    }

    public Mono<Void> updateHASAccount(UpdateHASUserRequest request) {
        return webClient
                .post()
                .uri(uriBuilder -> uriBuilder.path(HAS_ACCOUNT_UPDATE).build())
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .body(Mono.just(request), UpdateHASUserRequest.class)
                .accept(APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .toBodilessEntity()
                .then();
    }
}