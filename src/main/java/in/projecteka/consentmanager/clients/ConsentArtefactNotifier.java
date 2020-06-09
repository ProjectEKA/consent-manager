package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.properties.GatewayServiceProperties;
import in.projecteka.consentmanager.consent.model.HIPConsentArtefactRepresentation;
import in.projecteka.consentmanager.consent.model.request.HIPNotificationRequest;
import in.projecteka.consentmanager.consent.model.request.HIUNotificationRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Supplier;

import static in.projecteka.consentmanager.clients.ClientError.unknownErrorOccurred;
import static in.projecteka.consentmanager.clients.HeaderConstants.HDR_HIP_ID;

@AllArgsConstructor
public class ConsentArtefactNotifier {
    private final WebClient.Builder webClientBuilder;
    private final Supplier<Mono<String>> tokenGenerator;
    private final GatewayServiceProperties gatewayServiceProperties;

    private static final String HIP_CONSENT_NOTIFICATION_URL_PATH = "/consents/hip/notify";

    public Mono<Void> notifyHiu(HIUNotificationRequest request, String consentNotificationUrl) {
        return post(request, consentNotificationUrl);
    }

    public Mono<Void> sendConsentArtefactTo(HIPConsentArtefactRepresentation consentArtefact, String providerUrl) {
        String hipNotificationUrl = String.format("%s/%s", providerUrl, "consent/notification/");
        return post(consentArtefact, hipNotificationUrl);
    }

    public Mono<Void> sendConsentArtefactToHIP(HIPNotificationRequest notificationRequest, String hipId) {
        return postConsentArtefactToHip(notificationRequest, hipId);
    }

    private Mono<Void> post(Object body, String uri) {
        return tokenGenerator.get()
                .flatMap(token ->
                        webClientBuilder.build()
                                .post()
                                .uri(uri)
                                .header(HttpHeaders.AUTHORIZATION, token)
                                .bodyValue(body)
                                .retrieve()
                                .onStatus(HttpStatus::is5xxServerError,
                                        clientResponse -> Mono.error(unknownErrorOccurred()))
                                .toBodilessEntity())
                .then();
    }

    private Mono<Void> postConsentArtefactToHip(Object body, String hipId) {
        return tokenGenerator.get()
                .flatMap(token ->
                        webClientBuilder.build()
                                .post()
                                .uri(gatewayServiceProperties.getBaseUrl() + HIP_CONSENT_NOTIFICATION_URL_PATH)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, token)
                                .header(HDR_HIP_ID, hipId)
                                .bodyValue(body)
                                .retrieve()
                                .onStatus(httpStatus -> httpStatus.value() == 401,
                                        clientResponse -> Mono.error(ClientError.unAuthorized()))
                                .onStatus(HttpStatus::is5xxServerError,
                                        clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                                .toBodilessEntity()
                                .timeout(Duration.ofMillis(gatewayServiceProperties.getRequestTimeout())))
                .then();
    }
}
