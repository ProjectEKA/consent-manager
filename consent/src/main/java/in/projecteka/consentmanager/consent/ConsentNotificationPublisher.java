package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.consent.model.ConsentArtefactsMessage;
import in.projecteka.library.common.TraceableMessage;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.AmqpTemplate;
import reactor.core.publisher.Mono;

import static in.projecteka.consentmanager.Constants.HIP_CONSENT_NOTIFICATION_QUEUE;
import static in.projecteka.consentmanager.Constants.HIU_CONSENT_NOTIFICATION_QUEUE;
import static in.projecteka.library.common.Constants.CORRELATION_ID;

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

        sendMessage(message, destinationInfo.getExchange(), destinationInfo.getRoutingKey());
        logger.info("Broadcasting consent artefact notification for Request Id: {}",
                message.getConsentRequestId());
    }

    @SneakyThrows
    private void broadcastArtefactsToHips(ConsentArtefactsMessage message) {
        DestinationsConfig.DestinationInfo destinationInfo = destinationsConfig.getQueues()
                .get(HIP_CONSENT_NOTIFICATION_QUEUE);

        message.getConsentArtefacts()
                .forEach(consentArtefact -> {
                    sendMessage(consentArtefact, destinationInfo.getExchange(), destinationInfo.getRoutingKey());
                    logger.info(
                            "Broadcasting consent artefact notification to hip for consent artefact: {}",
                            consentArtefact.getConsentId()
                    );
                });
    }

    private void sendMessage(Object message, String exchange, String routingKey) {
        TraceableMessage traceableMessage = TraceableMessage.builder()
                .correlationId(MDC.get(CORRELATION_ID))
                .message(message)
                .build();
        amqpTemplate.convertAndSend(exchange, routingKey, traceableMessage);
    }
}
