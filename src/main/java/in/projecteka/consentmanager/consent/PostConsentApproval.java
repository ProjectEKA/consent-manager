package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.consent.model.ConsentArtefactsNotificationMessage;
import in.projecteka.consentmanager.consent.model.request.ConsentArtefactNotificationRequest;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactReference;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.amqp.core.AmqpTemplate;
import reactor.core.publisher.Mono;

import java.util.List;

import static in.projecteka.consentmanager.ConsentManagerConfiguration.CONSENT_GRANTED_QUEUE;

@AllArgsConstructor
public class PostConsentApproval {
    private AmqpTemplate amqpTemplate;
    private DestinationsConfig destinationsConfig;

    @SneakyThrows
    public Mono<Void> broadcastConsentArtefacts(String callBackUrl,
                                                List<ConsentArtefactReference> consents,
                                                String requestId) {
        DestinationsConfig.DestinationInfo destinationInfo = destinationsConfig.getQueues().get(CONSENT_GRANTED_QUEUE);

        if (destinationInfo == null) {
            return Mono.error(new Exception("Queue doesn't exists"));
        }

        return Mono.create(monoSink -> {
            amqpTemplate.convertAndSend(
                    destinationInfo.getExchange(),
                    destinationInfo.getRoutingKey(),
                    ConsentArtefactsNotificationMessage.builder()
                            .consentArtefactNotificationRequest(ConsentArtefactNotificationRequest.builder()
                                    .consentRequestId(requestId)
                                    .consents(consents)
                                    .build())
                            .callBackUrl(callBackUrl)
                            .build());
            monoSink.success();
        });
    }
}
