package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.user.KeycloakProperties;
import in.projecteka.consentmanager.user.model.KeycloakCreateUserRequest;
import in.projecteka.consentmanager.user.model.KeycloakToken;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class KeycloakClient {

    private final WebClient.Builder webClientBuilder;
    private final KeycloakProperties keyCloakProperties;

    public KeycloakClient(WebClient.Builder webClientBuilder,
                            KeycloakProperties keycloakProperties) {
        this.webClientBuilder = webClientBuilder;
        this.keyCloakProperties = keycloakProperties;
        this.webClientBuilder.baseUrl(keycloakProperties.getBaseUrl());

    }

    public Mono<?> createUser(KeycloakToken keycloakToken, KeycloakCreateUserRequest request) {
        String accessToken = String.format("Bearer %s", keycloakToken.getAccessToken());
        return webClientBuilder.build()
                .post()
                .uri(uriBuilder ->
                        uriBuilder.path("/admin/realms/consent-manager/users").build())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("Authorization", accessToken)
                .accept( MediaType.APPLICATION_JSON )
                .body(Mono.just(request), KeycloakCreateUserRequest.class)
                .retrieve()
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .toBodilessEntity()
                .then();
    }

    public Mono<KeycloakToken> getToken(MultiValueMap<String, String> formData) {
        return webClientBuilder.build()
                .post()
                .uri(uriBuilder ->
                        uriBuilder.path("/realms/consent-manager/protocol/openid-connect/token").build())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept( MediaType.APPLICATION_JSON )
                .body( BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .bodyToMono(KeycloakToken.class);
    }
}
