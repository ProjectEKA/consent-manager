package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.model.PatientLinkReferenceRequest;
import in.projecteka.consentmanager.clients.properties.GatewayServiceProperties;
import in.projecteka.consentmanager.common.Constants;
import in.projecteka.consentmanager.common.ServiceAuthentication;
import in.projecteka.consentmanager.link.link.model.LinkConfirmationRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
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

    private String getLinkConfirmationUrl() {
        return String.format(Constants.PATIENTS_CARE_CONTEXTS_LINK_CONFIRMATION_URL_PATH, gatewayServiceProperties.getBaseUrl());
    }

    private String getLinkEnquiryUrl() {
        return String.format(Constants.PATIENTS_CARE_CONTEXTS_LINK_INIT_URL_PATH, gatewayServiceProperties.getBaseUrl());
    }
}
