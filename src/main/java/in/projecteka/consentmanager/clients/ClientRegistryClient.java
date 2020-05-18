package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.model.Provider;
import in.projecteka.consentmanager.clients.model.Session;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Properties;

import static java.lang.String.format;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;

public class ClientRegistryClient {

    private final Logger logger = LoggerFactory.getLogger(ClientRegistryClient.class);
    private final WebClient.Builder webClientBuilder;

    public ClientRegistryClient(WebClient.Builder webClientBuilder, String baseUrl) {
        this.webClientBuilder = webClientBuilder.baseUrl(baseUrl);
    }

    public Flux<Provider> providersOf(String name, String authorization) {
        return webClientBuilder.build()
                .get()
                .uri(format("/api/2.0/providers?name=%s", name))
                .header(AUTHORIZATION, authorization)
                .retrieve()
                .bodyToFlux(Provider.class);
    }

    public Mono<Provider> providerWith(String id, String authorization) {
        return webClientBuilder.build()
                .get()
                .uri(format("/api/2.0/providers/%s", id))
                .header(AUTHORIZATION, authorization)
                .retrieve()
                .onStatus(httpStatus -> httpStatus.value() == 404,
                        clientResponse -> Mono.error(ClientError.providerNotFound()))
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .bodyToMono(Provider.class);
    }

    public Mono<Session> getTokenFor(String clientId, String clientSecret) {
        return webClientBuilder.build()
                .post()
                .uri("/api/1.0/sessions")
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestWith(clientId, clientSecret)))
                .retrieve()
                .onStatus(HttpStatus::isError, clientResponse -> clientResponse.bodyToMono(Properties.class)
                        .doOnNext(properties -> logger.error(properties.toString()))
                        .thenReturn(ClientError.unAuthorized()))
                .bodyToMono(Session.class);
    }

    public Mono<Session> getTokenWithRefreshToken(String clientId, String clientSecret, String refreshToken) {
        return webClientBuilder.build()
                .post()
                .uri("/api/1.0/sessionsRefresh")
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestWithRefreshToken(clientId, clientSecret, refreshToken)))
                .retrieve()
                .onStatus(HttpStatus::isError, clientResponse -> clientResponse.bodyToMono(Properties.class)
                        .doOnNext(properties -> logger.error(properties.toString()))
                        .thenReturn(ClientError.unAuthorized()))
                .bodyToMono(Session.class);
    }

    private SessionRequest requestWith(String clientId, String clientSecret) {
        return new SessionRequest(clientId, clientSecret, "password");
    }

    private SessionRequestWithRefreshToken requestWithRefreshToken(String clientId, String clientSecret, String refreshToken) {
        return new SessionRequestWithRefreshToken(clientId, clientSecret, "refresh_token", refreshToken);
    }

    @AllArgsConstructor
    @Data
    private static class SessionRequest {
        private String clientId;
        private String clientSecret;
        private String grantType;
    }

    @AllArgsConstructor
    @Data
    private static class SessionRequestWithRefreshToken {
        private String clientId;
        private String clientSecret;
        private String grantType;
        private String refreshToken;
    }
}
