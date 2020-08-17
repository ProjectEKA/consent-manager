package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.consent.model.ConsentArtefactResult;
import in.projecteka.consentmanager.consent.model.response.ConsentRequestResult;
import in.projecteka.consentmanager.dataflow.model.ConsentArtefactRepresentation;
import in.projecteka.consentmanager.properties.GatewayServiceProperties;
import in.projecteka.library.clients.model.ClientError;
import in.projecteka.library.common.ServiceAuthentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Supplier;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static in.projecteka.consentmanager.Constants.HDR_HIU_ID;
import static java.lang.String.format;
import static java.util.function.Predicate.not;

public class ConsentManagerClient {
    private static final String CONSENT_REQUEST_INIT_URL_PATH = "/consent-requests/on-init";
    private static final String CONSENT_FETCH_URL_PATH = "/consents/on-fetch";
    private final WebClient webClient;
    private final String url;

    public ConsentManagerClient(WebClient.Builder webClient,
                                String url, Supplier<Mono<String>> tokenGenerator,
                                GatewayServiceProperties gatewayServiceProperties,
                                ServiceAuthentication serviceAuthentication) {
        this.webClient = webClient.build();
        this.url = url;
        this.tokenGenerator = tokenGenerator;
        this.gatewayServiceProperties = gatewayServiceProperties;
        this.serviceAuthentication = serviceAuthentication;
    }

    private final Supplier<Mono<String>> tokenGenerator;
    private final GatewayServiceProperties gatewayServiceProperties;
    private final ServiceAuthentication serviceAuthentication;

    public Mono<ConsentArtefactRepresentation> getConsentArtefact(String consentArtefactId) {
        return tokenGenerator.get()
                .flatMap(token -> webClient
                        .get()
                        .uri(format("%s/internal/consents/%s", url, consentArtefactId))
                        .header("Authorization", token)
                        .retrieve()
                        .onStatus(not(HttpStatus::is2xxSuccessful),
                                clientResponse -> Mono.error(ClientError.consentArtefactNotFound()))
                        .bodyToMono(ConsentArtefactRepresentation.class));
    }

    public Mono<Void> sendInitResponseToGateway(ConsentRequestResult consentRequestResult, String hiuId) {
        return serviceAuthentication.authenticate()
                .flatMap(token ->
                        webClient
                                .post().uri(gatewayServiceProperties.getBaseUrl() + CONSENT_REQUEST_INIT_URL_PATH)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(AUTHORIZATION, token)
                                .header(HDR_HIU_ID, hiuId)
                                .bodyValue(consentRequestResult)
                                .retrieve()
                                .onStatus(httpStatus -> httpStatus.value() == 400,
                                        clientResponse -> Mono.error(ClientError.unprocessableEntity()))
                                .onStatus(httpStatus -> httpStatus.value() == 401,
                                        clientResponse -> Mono.error(ClientError.unAuthorized()))
                                .onStatus(HttpStatus::is5xxServerError,
                                        clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                                .toBodilessEntity()
                                .timeout(Duration.ofMillis(gatewayServiceProperties.getRequestTimeout())))
                .then();
    }

    public Mono<Void> sendConsentArtefactResponseToGateway(ConsentArtefactResult consentArtefactResult, String hiuId) {
        return serviceAuthentication.authenticate()
                .flatMap(token ->
                        webClient
                                .post()
                                .uri(gatewayServiceProperties.getBaseUrl() + CONSENT_FETCH_URL_PATH)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(AUTHORIZATION, token)
                                .header(HDR_HIU_ID, hiuId)
                                .bodyValue(consentArtefactResult)
                                .retrieve()
                                .onStatus(httpStatus -> httpStatus.value() == 400,
                                        clientResponse -> Mono.error(ClientError.unprocessableEntity()))
                                .onStatus(httpStatus -> httpStatus.value() == 401,
                                        clientResponse -> Mono.error(ClientError.unAuthorized()))
                                .onStatus(HttpStatus::is5xxServerError,
                                        clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                                .toBodilessEntity()
                                .timeout(Duration.ofMillis(gatewayServiceProperties.getRequestTimeout())))
                .then();
    }
}