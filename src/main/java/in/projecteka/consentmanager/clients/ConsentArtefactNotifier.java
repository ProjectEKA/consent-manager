package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.consent.model.HIPConsentArtefactRepresentation;
import in.projecteka.consentmanager.consent.model.request.HIUNotificationRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

import static in.projecteka.consentmanager.clients.ClientError.unknownErrorOccurred;

@AllArgsConstructor
public class ConsentArtefactNotifier {
    private final WebClient.Builder webClientBuilder;
    private final Supplier<Mono<String>> tokenGenerator;

    public Mono<Void> notifyHiu(HIUNotificationRequest request, String consentNotificationUrl) {
        return post(request, consentNotificationUrl);
    }

    public Mono<Void> sendConsentArtefactTo(HIPConsentArtefactRepresentation consentArtefact, String providerUrl) {
        String hipNotificationUrl = String.format("%s/%s", providerUrl, "consent/");
        return post(consentArtefact, hipNotificationUrl);
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
}
