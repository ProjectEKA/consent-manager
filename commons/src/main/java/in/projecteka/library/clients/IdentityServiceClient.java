package in.projecteka.library.clients;

import in.projecteka.library.clients.model.ClientError;
import in.projecteka.library.clients.model.KeyCloakUserCredentialRepresentation;
import in.projecteka.library.clients.model.KeyCloakUserPasswordChangeRequest;
import in.projecteka.library.clients.model.KeyCloakUserRepresentation;
import in.projecteka.library.clients.model.KeycloakUser;
import in.projecteka.library.clients.model.Session;
import in.projecteka.library.common.KeyCloakError;
import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.function.Function;

import static java.lang.String.format;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.reactive.function.BodyInserters.fromFormData;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.just;

public class IdentityServiceClient {

    private final WebClient webClient;

    public IdentityServiceClient(WebClient.Builder webClient, String baseUrl) {
        this.webClient = webClient.baseUrl(baseUrl).build();
    }

    public Mono<Void> createUser(Session session, KeycloakUser request) {
        String accessToken = format("Bearer %s", session.getAccessToken());
        return webClient
                .post()
                .uri(uriBuilder ->
                        uriBuilder.path("/admin/realms/consent-manager/users").build())
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .header(AUTHORIZATION, accessToken)
                .accept(APPLICATION_JSON)
                .body(just(request), KeycloakUser.class)
                .retrieve()
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .toBodilessEntity()
                .then();
    }

    public Mono<Session> getToken(MultiValueMap<String, String> formData) {
        return webClient
                .post()
                .uri(uriBuilder ->
                        uriBuilder.path("/realms/consent-manager/protocol/openid-connect/token").build())
                .contentType(APPLICATION_FORM_URLENCODED)
                .accept(APPLICATION_JSON)
                .body(fromFormData(formData))
                .retrieve()
                .onStatus(httpStatus -> httpStatus.value() == 401,
                        clientResponse -> clientResponse.bodyToMono(KeyCloakError.class).flatMap(toClientError()))
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .bodyToMono(Session.class);
    }

    private Function<KeyCloakError, Mono<ClientError>> toClientError() {
        return keyCloakError -> {
            String keyCloakErrorValue = keyCloakError.getError();
            switch (keyCloakErrorValue) {
                case "1002":
                    return Mono.error(ClientError.invalidOtp());
                case "1003":
                    return Mono.error(ClientError.otpExpired());
                default:
                    return Mono.error(ClientError.unknownUnauthorizedError(keyCloakError.getErrorDescription()));
            }
        };
    }

    public Mono<KeyCloakUserRepresentation> getUser(String userName, String accessToken) {
        String uri = format("/admin/realms/consent-manager/users?username=%s", userName);
        return webClient
                .get()
                .uri(uri)
                .header(AUTHORIZATION, accessToken)
                .accept(APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, clientResponse -> Mono.error(ClientError.userNotFound()))
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .bodyToFlux(KeyCloakUserRepresentation.class)
                .singleOrEmpty();
    }

    public Mono<KeyCloakUserCredentialRepresentation> getCredentials(String userId, String accessToken) {
        String uri = format("/admin/realms/consent-manager/users/%s/credentials", userId);
        return webClient
                .get()
                .uri(uri)
                .header(AUTHORIZATION, accessToken)
                .accept(APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, clientResponse -> Mono.error(ClientError.userNotFound()))
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .bodyToFlux(KeyCloakUserCredentialRepresentation.class)
                .singleOrEmpty();
    }

    public Mono<Void> logout(MultiValueMap<String, String> formData) {
        return webClient
                .post()
                .uri(uriBuilder ->
                        uriBuilder.path("/realms/consent-manager/protocol/openid-connect/logout").build())
                .contentType(APPLICATION_FORM_URLENCODED)
                .body(fromFormData(formData))
                .retrieve()
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .bodyToMono(Void.class);
    }

    public Mono<Void> updateUser(String accessToken, String keyCloakUserId, String userPassword) {
        String uri = format("/admin/realms/consent-manager/users/%s/reset-password", keyCloakUserId);
        KeyCloakUserPasswordChangeRequest keyCloakUserPasswordChangeRequest = KeyCloakUserPasswordChangeRequest
                .builder()
                .value(userPassword)
                .build();
        return webClient
                .put()
                .uri(uriBuilder ->
                        uriBuilder.path(uri).build())
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .header(AUTHORIZATION, accessToken)
                .body(just(keyCloakUserPasswordChangeRequest), KeyCloakUserPasswordChangeRequest.class)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, clientResponse -> Mono.error(ClientError.userNotFound()))
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .toBodilessEntity()
                .then();
    }
}
