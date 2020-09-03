package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.consent.model.ConsentArtefactResult;
import in.projecteka.consentmanager.consent.model.response.ConsentRequestResult;
import in.projecteka.consentmanager.consent.model.response.ConsentStatusResponse;
import in.projecteka.consentmanager.properties.GatewayServiceProperties;
import in.projecteka.library.clients.model.ClientError;
import in.projecteka.library.common.ServiceAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Properties;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static in.projecteka.consentmanager.Constants.HDR_HIU_ID;

public class ConsentManagerClient {
    private static final Logger logger = LoggerFactory.getLogger(ConsentManagerClient.class);
    private static final String CONSENT_REQUEST_INIT_URL_PATH = "/consent-requests/on-init";
    private static final String CONSENT_FETCH_URL_PATH = "/consents/on-fetch";
    private static final String CONSENT_REQUEST_STATUS_URL_PATH = "/consent-requests/on-status";
    private final WebClient webClient;

    public ConsentManagerClient(WebClient.Builder webClient,
                                String url,
                                GatewayServiceProperties gatewayServiceProperties,
                                ServiceAuthentication serviceAuthentication) {
        this.webClient = webClient.build();
        this.gatewayServiceProperties = gatewayServiceProperties;
        this.serviceAuthentication = serviceAuthentication;
    }

    private final GatewayServiceProperties gatewayServiceProperties;
    private final ServiceAuthentication serviceAuthentication;

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

    public Mono<Void> sendConsentStatusResponseToGateway(ConsentStatusResponse consentStatusResponse, String hiuId) {
        return serviceAuthentication.authenticate()
                .flatMap(token ->
                        webClient
                                .post()
                                .uri(gatewayServiceProperties.getBaseUrl() + CONSENT_REQUEST_STATUS_URL_PATH)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(AUTHORIZATION, token)
                                .header(HDR_HIU_ID, hiuId)
                                .bodyValue(consentStatusResponse)
                                .retrieve()
                                .onStatus(httpStatus -> httpStatus.value() == HttpStatus.BAD_REQUEST.value(),
                                        clientResponse -> clientResponse.bodyToMono(Properties.class)
                                                .doOnNext(properties -> logger.error(properties.toString()))
                                                .then(Mono.error(ClientError.unprocessableEntity())))
                                .onStatus(httpStatus -> httpStatus.value() == HttpStatus.UNAUTHORIZED.value(),
                                        clientResponse -> clientResponse.bodyToMono(Properties.class)
                                                .doOnNext(properties -> logger.error(properties.toString()))
                                                .then(Mono.error(ClientError.unAuthorized())))
                                .onStatus(HttpStatus::is5xxServerError,
                                        clientResponse -> clientResponse.bodyToMono(Properties.class)
                                                .doOnNext(properties -> logger.error(properties.toString()))
                                                .then(Mono.error(ClientError.networkServiceCallFailed())))
                                .toBodilessEntity()
                                .timeout(Duration.ofMillis(gatewayServiceProperties.getRequestTimeout())))
                .then();
    }
}