package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.model.KeyCloakUserCredentialRepresentation;
import in.projecteka.consentmanager.clients.model.KeyCloakUserPasswordChangeRequest;
import in.projecteka.consentmanager.clients.model.KeyCloakUserRepresentation;
import in.projecteka.consentmanager.clients.model.KeycloakUser;
import in.projecteka.consentmanager.clients.model.Session;
import in.projecteka.consentmanager.clients.properties.IdentityServiceProperties;
import in.projecteka.consentmanager.user.model.KeyCloakError;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static in.projecteka.consentmanager.clients.ClientError.invalidOtp;
import static in.projecteka.consentmanager.clients.ClientError.networkServiceCallFailed;
import static in.projecteka.consentmanager.clients.ClientError.otpExpired;
import static in.projecteka.consentmanager.clients.ClientError.unknownUnauthroziedError;
import static in.projecteka.consentmanager.clients.ClientError.userNotFound;
import static java.lang.String.format;

public class IdentityServiceClient {

    private final WebClient.Builder webClientBuilder;

    public IdentityServiceClient(WebClient.Builder webClientBuilder,
                                 IdentityServiceProperties identityServiceProperties) {
        this.webClientBuilder = webClientBuilder;
        this.webClientBuilder.baseUrl(identityServiceProperties.getBaseUrl());
    }

    public Mono<Void> createUser(Session session, KeycloakUser request) {
        String accessToken = format("Bearer %s", session.getAccessToken());
        return webClientBuilder.build()
                .post()
                .uri(uriBuilder ->
                        uriBuilder.path("/admin/realms/consent-manager/users").build())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(request), KeycloakUser.class)
                .retrieve()
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(networkServiceCallFailed()))
                .toBodilessEntity()
                .then();
    }

    public Mono<Session> getToken(MultiValueMap<String, String> formData) {
        return webClientBuilder.build()
                .post()
                .uri(uriBuilder ->
                        uriBuilder.path("/realms/consent-manager/protocol/openid-connect/token").build())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(httpStatus -> httpStatus.value() == 401,
                        clientResponse -> clientResponse.bodyToMono(KeyCloakError.class)
                                .flatMap(keyCloakError -> {
                                    String keyCloakErrorValue = keyCloakError.getError();
                                    switch (keyCloakErrorValue) {
                                        case "1002":
                                            return Mono.error(invalidOtp());
                                        case "1003":
                                            return Mono.error(otpExpired());
                                        default:
                                            return Mono.error(unknownUnauthroziedError(keyCloakError.getErrorDescription()));
                                    }
                                }))
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .bodyToMono(Session.class);
    }

    public Mono<KeyCloakUserRepresentation> getUser(String userName, String accessToken) {
        String uri = format("/admin/realms/consent-manager/users?username=%s", userName);
        return webClientBuilder.build()
                .get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, clientResponse -> Mono.error(userNotFound()))
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(networkServiceCallFailed()))
                .bodyToFlux(KeyCloakUserRepresentation.class)
                .singleOrEmpty();
    }

    public Mono<KeyCloakUserCredentialRepresentation> getCredentials(String userId, String accessToken) {
        String uri = format("/admin/realms/consent-manager/users/%s/credentials", userId);
        return webClientBuilder.build()
                .get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, clientResponse -> Mono.error(userNotFound()))
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(networkServiceCallFailed()))
                .bodyToFlux(KeyCloakUserCredentialRepresentation.class)
                .singleOrEmpty();
    }

    public Mono<Void> logout(MultiValueMap<String, String> formData) {
        return webClientBuilder.build()
                .post()
                .uri(uriBuilder ->
                        uriBuilder.path("/realms/consent-manager/protocol/openid-connect/logout").build())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(networkServiceCallFailed()))
                .bodyToMono(Void.class);
    }

    public Mono<Void> updateUser(String accessToken, String keyCloakUserId, String userPassword) {
        String uri = format("/admin/realms/consent-manager/users/%s/reset-password", keyCloakUserId);
        KeyCloakUserPasswordChangeRequest keyCloakUserPasswordChangeRequest = KeyCloakUserPasswordChangeRequest
                .builder()
                .value(userPassword)
                .build();
        return webClientBuilder.build()
                .put()
                .uri(uriBuilder ->
                        uriBuilder.path(uri).build())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .body(Mono.just(keyCloakUserPasswordChangeRequest), KeyCloakUserPasswordChangeRequest.class)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, clientResponse -> Mono.error(userNotFound()))
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(networkServiceCallFailed()))
                .toBodilessEntity()
                .then();
    }
}
