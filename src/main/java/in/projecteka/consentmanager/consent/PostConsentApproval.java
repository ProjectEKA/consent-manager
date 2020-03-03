package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.consent.model.ConsentArtefactsMessage;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.log4j.Logger;
import org.springframework.amqp.core.AmqpTemplate;
import reactor.core.publisher.Mono;

import static in.projecteka.consentmanager.ConsentManagerConfiguration.HIP_CONSENT_NOTIFICATION_QUEUE;
import static in.projecteka.consentmanager.ConsentManagerConfiguration.HIU_CONSENT_NOTIFICATION_QUEUE;

@AllArgsConstructor
public class PostConsentApproval {
    private static final Logger logger = Logger.getLogger(PostConsentApproval.class);
    private AmqpTemplate amqpTemplate;
    private DestinationsConfig destinationsConfig;

    public Mono<Void> broadcastConsentArtefacts(ConsentArtefactsMessage message){
        return Mono.create(monoSink -> {
            broadcastArtefactsToHiu(message);
            broadcastArtefactsToHips(message);
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
        logger.info(String.format("Broadcasting consent artefact notification for Request Id: %s",
                message.getRequestId()));
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
                    logger.info(String.format(
                            "Broadcasting consent artefact notification to hip for consent artefact: %s",
                            consentArtefact.getConsentDetail().getConsentId()));
                });
    }

    private void sendMessage(Object message, String exchange, String routingKey) {
        amqpTemplate.convertAndSend(exchange, routingKey, message);
    }
}
