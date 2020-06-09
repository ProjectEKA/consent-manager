package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.properties.GatewayServiceProperties;
import in.projecteka.consentmanager.common.CentralRegistry;
import in.projecteka.consentmanager.consent.model.ConsentArtefactResult;
import in.projecteka.consentmanager.consent.model.response.ConsentRequestResult;
import in.projecteka.consentmanager.dataflow.model.ConsentArtefactRepresentation;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Supplier;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static java.lang.String.format;
import static java.util.function.Predicate.not;

@AllArgsConstructor
public class ConsentManagerClient {
    private static final String CONSENT_REQUEST_INIT_URL_PATH = "/consent-requests/on-init";
    private static final String CONSENT_FETCH_URL_PATH = "/consents/on-fetch";
    private final WebClient.Builder webClientBuilder;
    private final String url;
    private final Supplier<Mono<String>> tokenGenerator;
    private final GatewayServiceProperties gatewayServiceProperties;
    private final CentralRegistry centralRegistry;


    public Mono<ConsentArtefactRepresentation> getConsentArtefact(String consentArtefactId) {
        return tokenGenerator.get()
                .flatMap(token -> webClientBuilder.build()
                        .get()
                        .uri(format("%s/internal/consents/%s", url, consentArtefactId))
                        .header("Authorization", token)
                        .retrieve()
                        .onStatus(not(HttpStatus::is2xxSuccessful),
                                clientResponse -> Mono.error(ClientError.consentArtefactNotFound()))
                        .bodyToMono(ConsentArtefactRepresentation.class));
    }

    public Mono<Void> sendInitResponseToGateway(ConsentRequestResult consentRequestResult, String hiuId) {
        return tokenGenerator.get()
                .flatMap(token ->
                        webClientBuilder.build()
                                .post().uri(gatewayServiceProperties.getBaseUrl() + CONSENT_REQUEST_INIT_URL_PATH)
                                .contentType(MediaType.APPLICATION_JSON)
                        .header(AUTHORIZATION, token)
                        .header("X-HIU-ID", hiuId)
                        .bodyValue(consentRequestResult)
                        .retrieve()
                        .onStatus(httpStatus -> httpStatus.value() == 400,
                                clientResponse -> Mono.error(ClientError.unprocessableEntity()))
                        .onStatus(httpStatus -> httpStatus.value() == 401,
                                clientResponse -> Mono.error(ClientError.unAuthorized()))
                        .onStatus(HttpStatus::is5xxServerError,
                                clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                        .toBodilessEntity()
                        .timeout(Duration.ofMillis(gatewayServiceProperties.getRequestTimeout()))
                ).then();
    }

    public Mono<Void> sendConsentArtefactResponseToGateway(ConsentArtefactResult consentArtefactResult, String hiuId) {
        return centralRegistry.authenticate()
                .flatMap(token ->
                        webClientBuilder.build()
                                .post()
                                .uri(gatewayServiceProperties.getBaseUrl() + CONSENT_FETCH_URL_PATH)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(AUTHORIZATION, token)
                                .header("X-HIU-ID", hiuId)
                                .bodyValue(consentArtefactResult)
                                .retrieve()
                                .onStatus(httpStatus -> httpStatus.value() == 400,
                                        clientResponse -> Mono.error(ClientError.unprocessableEntity()))
                                .onStatus(httpStatus -> httpStatus.value() == 401,
                                        clientResponse -> Mono.error(ClientError.unAuthorized()))
                                .onStatus(HttpStatus::is5xxServerError,
                                        clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                                .toBodilessEntity()
                                .timeout(Duration.ofMillis(gatewayServiceProperties.getRequestTimeout()))
                ).then();
    }
}