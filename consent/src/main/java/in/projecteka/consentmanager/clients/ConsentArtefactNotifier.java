package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.properties.GatewayServiceProperties;
import in.projecteka.consentmanager.consent.model.request.HIPNotificationRequest;
import in.projecteka.consentmanager.consent.model.request.HIUNotificationRequest;
import in.projecteka.library.clients.model.ClientError;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Supplier;

import static in.projecteka.consentmanager.Constants.HDR_HIP_ID;
import static in.projecteka.consentmanager.Constants.HDR_HIU_ID;
import static in.projecteka.library.common.Constants.CORRELATION_ID;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static reactor.core.publisher.Mono.error;

public class ConsentArtefactNotifier {
    private final WebClient webClient;
    private final Supplier<Mono<String>> tokenGenerator;
    private final GatewayServiceProperties gatewayServiceProperties;

    private static final String HIP_CONSENT_NOTIFICATION_URL_PATH = "/consents/hip/notify";
    private static final String HIU_CONSENT_NOTIFICATION_URL_PATH = "/consents/hiu/notify";

    public ConsentArtefactNotifier(WebClient.Builder webClient,
                                   Supplier<Mono<String>> tokenGenerator,
                                   GatewayServiceProperties gatewayServiceProperties) {
        this.webClient = webClient.build();
        this.tokenGenerator = tokenGenerator;
        this.gatewayServiceProperties = gatewayServiceProperties;
    }

    public Mono<Void> sendConsentArtifactToHIU(HIUNotificationRequest request, String hiuId) {
        return postConsentArtifactToHiu(request, hiuId);
    }

    public Mono<Void> sendConsentArtefactToHIP(HIPNotificationRequest notificationRequest, String hipId) {
        return postConsentArtefactToHip(notificationRequest, hipId);
    }

    private Mono<Void> postConsentArtifactToHiu(HIUNotificationRequest body, String hiuId) {
        return tokenGenerator.get()
                .flatMap(token -> webClient
                                .post()
                                .uri(gatewayServiceProperties.getBaseUrl() + HIU_CONSENT_NOTIFICATION_URL_PATH)
                                .header(AUTHORIZATION, token)
                                .header(HDR_HIU_ID, hiuId)
                                .header(CORRELATION_ID, MDC.get(CORRELATION_ID))
                                .bodyValue(body)
                                .retrieve()
                                .onStatus(httpStatus -> httpStatus.value() == 401,
                                        // Error msg should be logged
                                        clientResponse -> error(ClientError.unAuthorized()))
                                .onStatus(HttpStatus::is5xxServerError,
                                        clientResponse -> error(ClientError.networkServiceCallFailed()))
                                .toBodilessEntity())
                .timeout(Duration.ofMillis(gatewayServiceProperties.getRequestTimeout()))
                .then();
    }

    private Mono<Void> postConsentArtefactToHip(HIPNotificationRequest body, String hipId) {
        return tokenGenerator.get()
                .flatMap(token -> webClient
                                .post()
                                .uri(gatewayServiceProperties.getBaseUrl() + HIP_CONSENT_NOTIFICATION_URL_PATH)
                                .contentType(APPLICATION_JSON)
                                .header(AUTHORIZATION, token)
                                .header(HDR_HIP_ID, hipId)
                                .header(CORRELATION_ID, MDC.get(CORRELATION_ID))
                                .bodyValue(body)
                                .retrieve()
                                .onStatus(httpStatus -> httpStatus.value() == 401,
                                        // Error msg should be logged
                                        clientResponse -> error(ClientError.unAuthorized()))
                                .onStatus(HttpStatus::is5xxServerError,
                                        clientResponse -> error(ClientError.networkServiceCallFailed()))
                                .toBodilessEntity()
                                .timeout(Duration.ofMillis(gatewayServiceProperties.getRequestTimeout())))
                .then();
    }
}
