package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.consent.model.ConsentArtefactsNotificationMessage;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.log4j.Logger;
import org.springframework.amqp.core.AmqpTemplate;
import reactor.core.publisher.Mono;

import static in.projecteka.consentmanager.ConsentManagerConfiguration.CONSENT_GRANTED_QUEUE;

@AllArgsConstructor
public class PostConsentApproval {
    private static final Logger logger = Logger.getLogger(PostConsentApproval.class);
    private AmqpTemplate amqpTemplate;
    private DestinationsConfig destinationsConfig;

    @SneakyThrows
    public Mono<Void> broadcastConsentArtefacts(ConsentArtefactsNotificationMessage message) {
        DestinationsConfig.DestinationInfo destinationInfo = destinationsConfig.getQueues().get(CONSENT_GRANTED_QUEUE);

        if (destinationInfo == null) {
            logger.error(CONSENT_GRANTED_QUEUE + " not found");
            return Mono.error(new Exception(CONSENT_GRANTED_QUEUE + " not found"));

        }

        return Mono.create(monoSink -> {
            amqpTemplate.convertAndSend(destinationInfo.getExchange(), destinationInfo.getRoutingKey(), message);
            logger.info("Broadcasting consent artefact notification for Request Id: " + message.getRequestId());
            monoSink.success();
        });
    }
}
