package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.model.Session;
import in.projecteka.library.clients.model.ClientError;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Properties;

import static org.springframework.http.MediaType.APPLICATION_JSON;

public class ServiceAuthenticationClient {
    private final Logger logger = LoggerFactory.getLogger(ServiceAuthenticationClient.class);
    private final WebClient webClient;

    public ServiceAuthenticationClient(WebClient.Builder webClient, String baseUrl) {
        this.webClient = webClient.baseUrl(baseUrl).build();
    }

    public Mono<Session> getTokenFor(String clientId, String clientSecret) {
        return webClient
                .post()
                .uri("/sessions")
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestWith(clientId, clientSecret)))
                .retrieve()
                .onStatus(HttpStatus::isError, clientResponse -> clientResponse.bodyToMono(Properties.class)
                        .doOnNext(properties -> logger.error(properties.toString()))
                        .thenReturn(ClientError.unAuthorized()))
                .bodyToMono(Session.class);
    }

    private SessionRequest requestWith(String clientId, String clientSecret) {
        return new SessionRequest(clientId, clientSecret);
    }

    @AllArgsConstructor
    @Value
    private static class SessionRequest {
        String clientId;
        String clientSecret;
    }
}
