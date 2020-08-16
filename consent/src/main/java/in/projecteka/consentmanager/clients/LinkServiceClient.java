package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.model.PatientLinkReferenceRequest;
import in.projecteka.consentmanager.clients.properties.GatewayServiceProperties;
import in.projecteka.consentmanager.common.Constants;
import in.projecteka.consentmanager.common.ServiceAuthentication;
import in.projecteka.consentmanager.link.link.model.LinkConfirmationRequest;
import in.projecteka.consentmanager.link.link.model.LinkResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static in.projecteka.consentmanager.common.Constants.HDR_HIP_ID;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@AllArgsConstructor
public class LinkServiceClient {
    private final WebClient webClientBuilder;
    private final ServiceAuthentication serviceAuthentication;
    private final GatewayServiceProperties gatewayServiceProperties;

    public Mono<Boolean> linkPatientEnquiryRequest(PatientLinkReferenceRequest patientLinkReferenceRequest,
                                                   String authorization,
                                                   String hipId) {
        return webClientBuilder
                .post()
                .uri(getLinkEnquiryUrl())
                .header(AUTHORIZATION, authorization)
                .header(HDR_HIP_ID, hipId)
                .body(Mono.just(patientLinkReferenceRequest), PatientLinkReferenceRequest.class)
                .retrieve()
                .onStatus(httpStatus -> httpStatus.value() == 401,
                        // Error msg should be logged
                        clientResponse -> Mono.error(ClientError.unknownErrorOccurred()))
                .onStatus(HttpStatus::is5xxServerError,
                        clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .toBodilessEntity()
                .timeout(Duration.ofMillis(gatewayServiceProperties.getRequestTimeout()))
                .thenReturn(Boolean.TRUE);
    }

    public Mono<Boolean> confirmPatientLink(LinkConfirmationRequest confirmationRequest, String hipId) {
        return serviceAuthentication.authenticate()
                .flatMap(authToken ->
                        webClientBuilder
                                .post()
                                .uri(getLinkConfirmationUrl())
                                .header(AUTHORIZATION, authToken)
                                .header(HDR_HIP_ID, hipId)
                                .bodyValue(confirmationRequest)
                                .retrieve()
                                .onStatus(httpStatus -> httpStatus.value() == 401,
                                        // Error msg should be logged
                                        clientResponse -> Mono.error(ClientError.unknownErrorOccurred()))
                                .onStatus(httpStatus -> httpStatus.value() == 404,
                                        clientResponse -> Mono.error(ClientError.userNotFound()))
                                .onStatus(HttpStatus::is5xxServerError,
                                        clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                                .toBodilessEntity()
                                .timeout(Duration.ofMillis(gatewayServiceProperties.getRequestTimeout())))
                .thenReturn(Boolean.TRUE);
    }

    public Mono<Void> sendLinkResponseToGateway(LinkResponse linkResponse, String hipId) {
        return serviceAuthentication.authenticate()
                .flatMap(token ->
                        webClientBuilder
                                .post()
                                .uri(getLinkOnAddContextsUrl())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(AUTHORIZATION, token)
                                .header(HDR_HIP_ID, hipId)
                                .bodyValue(linkResponse)
                                .retrieve()
                                .onStatus(httpStatus -> httpStatus.value() == HttpStatus.BAD_REQUEST.value(),
                                        clientResponse -> Mono.error(ClientError.unprocessableEntity()))
                                .onStatus(httpStatus -> httpStatus.value() == HttpStatus.UNAUTHORIZED.value(),
                                        clientResponse -> Mono.error(ClientError.unAuthorized()))
                                .onStatus(HttpStatus::is5xxServerError,
                                        clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                                .toBodilessEntity()
                                .timeout(Duration.ofMillis(gatewayServiceProperties.getRequestTimeout())))
                .then();
    }

    private String getLinkOnAddContextsUrl() {
        return getBaseUrl().concat(Constants.LINKS_LINK_ON_ADD_CONTEXTS);
    }

    private String getLinkConfirmationUrl() {
        return getBaseUrl().concat(Constants.PATIENTS_CARE_CONTEXTS_LINK_CONFIRMATION_URL_PATH);
    }

    private String getLinkEnquiryUrl() {
        return getBaseUrl().concat(Constants.PATIENTS_CARE_CONTEXTS_LINK_INIT_URL_PATH);
    }

    private String getBaseUrl() {
        var baseUrl = gatewayServiceProperties.getBaseUrl();
        String trimmed = baseUrl.trim();
        if (trimmed.endsWith("/")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
