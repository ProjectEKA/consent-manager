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
            String errorMessage = String.format("%s %s", CONSENT_GRANTED_QUEUE, " not found");
            logger.error(errorMessage);
            return Mono.error(new Exception(errorMessage));

        }

        return Mono.create(monoSink -> {
            amqpTemplate.convertAndSend(destinationInfo.getExchange(), destinationInfo.getRoutingKey(), message);
            logger.info(String.format("Broadcasting consent artefact notification for Request Id: %s",
                    message.getRequestId()));
            monoSink.success();
        });
    }
}
