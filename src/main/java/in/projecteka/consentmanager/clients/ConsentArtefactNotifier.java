package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.consent.model.request.ConsentArtefactNotificationRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static in.projecteka.consentmanager.clients.ClientError.unknownErrorOccurred;

@AllArgsConstructor
public class ConsentArtefactNotifier {
    private WebClient.Builder webClientBuilder;

    public Mono<Void> notifyHiu(ConsentArtefactNotificationRequest consentArtefactNotificationRequest,
                                String callBackUrl) {
        return webClientBuilder.build()
                .post()
                .uri(callBackUrl + "/consent/notification/")
                .header(HttpHeaders.AUTHORIZATION, "bmNn")//TODO: change it to jwt token
                .bodyValue(consentArtefactNotificationRequest)
                .retrieve()
                .onStatus(HttpStatus::is5xxServerError, clientResponse -> Mono.error(unknownErrorOccurred()))
                .toBodilessEntity()
                .then();
    }
}
