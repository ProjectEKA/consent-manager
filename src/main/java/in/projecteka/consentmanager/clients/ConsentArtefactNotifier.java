package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.consent.model.HIPConsentArtefactRepresentation;
import in.projecteka.consentmanager.consent.model.request.HIUNotificationRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static in.projecteka.consentmanager.clients.ClientError.unknownErrorOccurred;

@AllArgsConstructor
public class ConsentArtefactNotifier {
    private WebClient.Builder webClientBuilder;

    public Mono<Void> notifyHiu(HIUNotificationRequest request, String callBackUrl) {
        String hiuNotificationUrl = String.format("%s/%s", callBackUrl, "consent/notification/");
        return post(request, hiuNotificationUrl);
    }

    public Mono<Void> sendConsentArtefactTo(HIPConsentArtefactRepresentation consentArtefact, String providerUrl) {
        String hipNotificationUrl = String.format("%s/%s", providerUrl, "consent/");
        return post(consentArtefact, hipNotificationUrl);
    }

    private Mono<Void> post(Object body, String uri) {
        return webClientBuilder.build()
                .post()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "bmNn")//TODO: change it to jwt token
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatus::is5xxServerError, clientResponse -> Mono.error(unknownErrorOccurred()))
                .toBodilessEntity()
                .then();
    }
}
