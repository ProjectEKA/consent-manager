package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.properties.OtpServiceProperties;
import in.projecteka.consentmanager.consent.model.NotificationMessage;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static in.projecteka.consentmanager.clients.ClientError.unknownErrorOccurred;

@AllArgsConstructor
public class ConsentNotificationClient {
    private OtpServiceProperties otpServiceProperties;
    private final WebClient.Builder webClientBuilder;

    public Mono<Void> sendToNotificationService(NotificationMessage notificationMessage) {
        return webClientBuilder.build()
                .post()
                .uri(otpServiceProperties.getUrl() + "/notification")
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(notificationMessage)
                .retrieve()
                .onStatus(HttpStatus::is5xxServerError, clientResponse -> Mono.error(unknownErrorOccurred()))
                .toBodilessEntity()
                .then();
    }
}
