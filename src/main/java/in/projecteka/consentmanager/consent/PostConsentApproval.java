package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.consent.model.ConsentArtefactsMessage;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.log4j.Logger;
import org.springframework.amqp.core.AmqpTemplate;
import reactor.core.publisher.Mono;

import static in.projecteka.consentmanager.ConsentManagerConfiguration.NOTIFY_CONSENT_TO_HIP_QUEUE;
import static in.projecteka.consentmanager.ConsentManagerConfiguration.NOTIFY_CONSENT_TO_HIU_QUEUE;

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
                .get(NOTIFY_CONSENT_TO_HIU_QUEUE);

        if (destinationInfo == null) {
            String errorMessage = String.format("%s %s", NOTIFY_CONSENT_TO_HIU_QUEUE, " not found");
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
                .get(NOTIFY_CONSENT_TO_HIP_QUEUE);

        if (destinationInfo == null) {
            String errorMessage = String.format("%s %s", NOTIFY_CONSENT_TO_HIP_QUEUE, " not found");
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
