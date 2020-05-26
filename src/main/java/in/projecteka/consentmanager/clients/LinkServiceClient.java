package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.model.ErrorRepresentation;
import in.projecteka.consentmanager.link.link.model.LinkConfirmationRequest;
import in.projecteka.consentmanager.clients.model.PatientLinkReferenceRequest;
import in.projecteka.consentmanager.clients.model.PatientLinkReferenceResponse;
import in.projecteka.consentmanager.clients.model.PatientLinkRequest;
import in.projecteka.consentmanager.clients.model.PatientLinkResponse;
import in.projecteka.consentmanager.clients.properties.GatewayServiceProperties;
import in.projecteka.consentmanager.common.CentralRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static java.util.function.Predicate.not;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

public class LinkServiceClient {

    private final WebClient.Builder webClientBuilder;
    private final CentralRegistry centralRegistry;
    private final GatewayServiceProperties gatewayServiceProperties;

    private static final String HDR_HIP_ID = "X-HIP-ID";
    private static final String PATIENTS_CARE_CONTEXTS_LINK_CONFIRMATION_URL_PATH = "%s/links/link/confirm";

    public LinkServiceClient(WebClient.Builder webClientBuilder, CentralRegistry centralRegistry, GatewayServiceProperties gatewayServiceProperties) {
        this.webClientBuilder = webClientBuilder;
        this.centralRegistry = centralRegistry;
        this.gatewayServiceProperties = gatewayServiceProperties;
    }

    public Mono<PatientLinkReferenceResponse> linkPatientEnquiry(
            PatientLinkReferenceRequest patientLinkReferenceRequest,
            String url,
            String authorization) {
        return webClientBuilder.build()
                .post()
                .uri(String.format("%s/patients/link", url))
                .header(AUTHORIZATION, authorization)
                .body(Mono.just(patientLinkReferenceRequest), PatientLinkReferenceRequest.class)
                .retrieve()
                .onStatus(not(HttpStatus::is2xxSuccessful), clientResponse ->
                        clientResponse.bodyToMono(ErrorRepresentation.class)
                                .flatMap(e -> Mono.error(new ClientError(clientResponse.statusCode(), e))))
                .bodyToMono(PatientLinkReferenceResponse.class);
    }

    public Mono<PatientLinkResponse> linkPatientConfirmation(
            String linkRefNumber,
            PatientLinkRequest patientLinkRequest,
            String url,
            String authorization) {
        return webClientBuilder.build()
                .post()
                .uri(String.format("%s/patients/link/%s", url, linkRefNumber))
                .header(AUTHORIZATION, authorization)
                .body(Mono.just(patientLinkRequest), PatientLinkRequest.class)
                .retrieve()
                .onStatus(not(HttpStatus::is2xxSuccessful), clientResponse ->
                        clientResponse.bodyToMono(ErrorRepresentation.class)
                                .flatMap(e -> Mono.error(new ClientError(clientResponse.statusCode(), e))))
                .bodyToMono(PatientLinkResponse.class);
    }



    public Mono<Boolean> confirmPatientLink(
            LinkConfirmationRequest confirmationRequest,
            String hipId) {
        return centralRegistry.authenticate()
                .flatMap(authToken ->
                    webClientBuilder.build()
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
                        .timeout(Duration.ofMillis(gatewayServiceProperties.getRequestTimeout()))
                ).thenReturn(Boolean.TRUE);
    }

    private String getLinkConfirmationUrl() {
        return String.format(PATIENTS_CARE_CONTEXTS_LINK_CONFIRMATION_URL_PATH, gatewayServiceProperties.getBaseUrl());
    }
}
