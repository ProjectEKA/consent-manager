package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.consent.model.ConsentArtefactsMessage;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.amqp.core.AmqpTemplate;
import reactor.core.publisher.Mono;

import static in.projecteka.consentmanager.ConsentManagerConfiguration.HIP_CONSENT_NOTIFICATION_QUEUE;
import static in.projecteka.consentmanager.ConsentManagerConfiguration.HIU_CONSENT_NOTIFICATION_QUEUE;

@AllArgsConstructor
public class ConsentNotificationPublisher {
    private static final Logger logger = LoggerFactory.getLogger(ConsentNotificationPublisher.class);
    private final AmqpTemplate amqpTemplate;
    private final DestinationsConfig destinationsConfig;

    public Mono<Void> publish(ConsentArtefactsMessage message) {
        return Mono.create(monoSink -> {
            broadcastArtefactsToHiu(message);
            broadcastArtefactsToHips(message);
            monoSink.success();
        });
    }

    @SneakyThrows
    private void broadcastArtefactsToHiu(ConsentArtefactsMessage message) {
        DestinationsConfig.DestinationInfo destinationInfo = destinationsConfig.getQueues()
                .get(HIU_CONSENT_NOTIFICATION_QUEUE);

        if (destinationInfo == null) {
            String errorMessage = String.format("%s %s", HIU_CONSENT_NOTIFICATION_QUEUE, " not found");
            logger.error(errorMessage);
            throw new Exception(errorMessage);
        }
        sendMessage(message, destinationInfo.getExchange(), destinationInfo.getRoutingKey());
        logger.info("Broadcasting consent artefact notification for Request Id: {}",
                message.getConsentRequestId());
    }

    @SneakyThrows
    private void broadcastArtefactsToHips(ConsentArtefactsMessage message) {
        DestinationsConfig.DestinationInfo destinationInfo = destinationsConfig.getQueues()
                .get(HIP_CONSENT_NOTIFICATION_QUEUE);

        if (destinationInfo == null) {
            String errorMessage = String.format("%s %s", HIP_CONSENT_NOTIFICATION_QUEUE, " not found");
            logger.error(errorMessage);
            throw new Exception(errorMessage);
        }
        message.getConsentArtefacts()
                .forEach(consentArtefact -> {
                    sendMessage(consentArtefact, destinationInfo.getExchange(), destinationInfo.getRoutingKey());
                    logger.info(
                            "Broadcasting consent artefact notification to hip for consent artefact: {}",
                            consentArtefact.getConsentDetail().getConsentId());
                });
    }

    private void sendMessage(Object message, String exchange, String routingKey) {
        amqpTemplate.convertAndSend(exchange, routingKey, message);
    }
}
