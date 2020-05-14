package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.model.KeyCloakUserPasswordChangeRequest;
import in.projecteka.consentmanager.clients.model.KeyCloakUserRepresentation;
import in.projecteka.consentmanager.clients.model.KeycloakUser;
import in.projecteka.consentmanager.clients.model.Session;
import in.projecteka.consentmanager.clients.properties.IdentityServiceProperties;
import in.projecteka.consentmanager.common.MonoVoidOperator;
import in.projecteka.consentmanager.user.model.KeyCloakError;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .toBodilessEntity()
                .then();
    }

    public Mono<Session> getToken(MultiValueMap<String, String> formData) {
        return getToken(formData, Mono::empty);
    }

    public Mono<Session> getToken(MultiValueMap<String, String> formData, MonoVoidOperator onInvalidOTP) {
        return webClientBuilder.build()
                .post()
                .uri(uriBuilder ->
                        uriBuilder.path("/realms/consent-manager/protocol/openid-connect/token").build())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(httpStatus -> httpStatus.value() == 401, clientResponse -> clientResponse.bodyToMono(KeyCloakError.class)
                        .flatMap(keyCloakError -> {
                            String keyCloakErrorValue = keyCloakError.getError();
                            switch (keyCloakErrorValue) {
                                case "1002":
                                    return onInvalidOTP.perform().then(Mono.error(ClientError.invalidOtp()));
                                case "1003":
                                    return Mono.error(ClientError.otpExpired());
                                default:
                                    return Mono.error(ClientError.unknownUnauthroziedError(keyCloakError.getErrorDescription()));
                            }
                        }))
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .bodyToMono(Session.class);
    }

    public Flux<KeyCloakUserRepresentation> getUser(String userName, String accessToken) {
        String uri = format("/admin/realms/consent-manager/users?username=%s", userName);
        return webClientBuilder.build()
                .get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatus::isError, clientResponse -> {
                    return Mono.error(ClientError.userNotFound());
                })
                .bodyToFlux(KeyCloakUserRepresentation.class);
    }

    public Mono<Void> logout(MultiValueMap<String, String> formData) {
        return webClientBuilder.build()
                .post()
                .uri(uriBuilder ->
                        uriBuilder.path("/realms/consent-manager/protocol/openid-connect/logout").build())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .bodyToMono(Void.class);
    }

    public Mono<Void> updateUser(Session session, String keyCloakUserId, String userPassword) {
        String accessToken = format("Bearer %s", session.getAccessToken());
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
                .onStatus(HttpStatus::is4xxClientError, clientResponse -> Mono.error(ClientError.userNotFound()))
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .toBodilessEntity()
                .then();
    }
}
