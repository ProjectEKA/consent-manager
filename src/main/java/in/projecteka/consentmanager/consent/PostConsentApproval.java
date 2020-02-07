package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.consent.model.ConsentArtefactsNotificationMessage;
import in.projecteka.consentmanager.consent.model.request.ConsentArtefactNotificationRequest;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactReference;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.log4j.Logger;
import org.springframework.amqp.core.AmqpTemplate;
import reactor.core.publisher.Mono;

import java.util.List;

import static in.projecteka.consentmanager.ConsentManagerConfiguration.CONSENT_GRANTED_QUEUE;

@AllArgsConstructor
public class PostConsentApproval {
    final static Logger logger = Logger.getLogger(PostConsentApproval.class);
    private AmqpTemplate amqpTemplate;
    private DestinationsConfig destinationsConfig;

    @SneakyThrows
    public Mono<Void> broadcastConsentArtefacts(String callBackUrl,
                                                List<ConsentArtefactReference> consents,
                                                String requestId) {
        DestinationsConfig.DestinationInfo destinationInfo = destinationsConfig.getQueues().get(CONSENT_GRANTED_QUEUE);

        if (destinationInfo == null) {
            logger.error(CONSENT_GRANTED_QUEUE + " not found");
            return Mono.error(new Exception(CONSENT_GRANTED_QUEUE + " not found"));

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
            logger.info("Broadcasting consent artefact notification for Request Id: " + requestId);
            monoSink.success();
        });
    }
}
