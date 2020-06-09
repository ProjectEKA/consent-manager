package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.MessageListenerContainerFactory;
import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.ConsentArtefactNotifier;
import in.projecteka.consentmanager.common.ListenerProperties;
import in.projecteka.consentmanager.consent.model.ConsentArtefactsMessage;
import in.projecteka.consentmanager.consent.model.request.ConsentArtefactReference;
import in.projecteka.consentmanager.consent.model.request.ConsentNotifier;
import in.projecteka.consentmanager.consent.model.request.HIUNotificationRequest;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.stream.Collectors;

import static in.projecteka.consentmanager.ConsentManagerConfiguration.HIU_CONSENT_NOTIFICATION_QUEUE;
import static in.projecteka.consentmanager.ConsentManagerConfiguration.PARKING_EXCHANGE;
import static in.projecteka.consentmanager.clients.ClientError.queueNotFound;

@AllArgsConstructor
public class HiuConsentNotificationListener {
    private static final Logger logger = LoggerFactory.getLogger(HiuConsentNotificationListener.class);
    private final MessageListenerContainerFactory messageListenerContainerFactory;
    private final DestinationsConfig destinationsConfig;
    private final Jackson2JsonMessageConverter converter;
    private final ConsentArtefactNotifier consentArtefactNotifier;
    private final AmqpTemplate amqpTemplate;
    private final ListenerProperties listenerProperties;

    @PostConstruct
    public void subscribe() throws ClientError {
        DestinationsConfig.DestinationInfo destinationInfo = destinationsConfig
                .getQueues()
                .get(HIU_CONSENT_NOTIFICATION_QUEUE);
        if (destinationInfo == null) {
            logger.error(HIU_CONSENT_NOTIFICATION_QUEUE + " not found");
            throw queueNotFound();
        }

        MessageListenerContainer mlc = messageListenerContainerFactory
                .createMessageListenerContainer(destinationInfo.getRoutingKey());

        MessageListener messageListener = message -> {
            try {
                //This is NOT a generic solution. Based on the context, it either needs to retry, or it might also need to propagate the error to the upstream systems.
                //TODO be revisited during Gateway development
                if (hasExceededRetryCount(message)) {
                    amqpTemplate.convertAndSend(PARKING_EXCHANGE, message.getMessageProperties().getReceivedRoutingKey(), message);
                    return;
                }
                ConsentArtefactsMessage consentArtefactsMessage =
                        (ConsentArtefactsMessage) converter.fromMessage(message);
                logger.info("Received message for Request id : {}", consentArtefactsMessage.getConsentRequestId());

                notifyHiu(consentArtefactsMessage);
            } catch (Exception e) {
                throw new AmqpRejectAndDontRequeueException(e.getMessage(), e);
            }

        };
        mlc.setupMessageListener(messageListener);

        mlc.start();
    }

    private boolean hasExceededRetryCount(Message in) {
        List<Map<String, ?>> xDeathHeader = in.getMessageProperties().getXDeathHeader();
        if (xDeathHeader != null && !xDeathHeader.isEmpty()) {
            Long count = (Long) xDeathHeader.get(0).get("count");
            logger.info("[HIU] Number of attempts {}", count);
            return count >= listenerProperties.getMaximumRetries();
        }
        return false;
    }

    private void notifyHiu(ConsentArtefactsMessage consentArtefactsMessage) {
        HIUNotificationRequest hiuNotificationRequest = hiuNotificationRequest(consentArtefactsMessage);
        String hiuId = consentArtefactsMessage.getHiuId();
        consentArtefactNotifier.sendConsentArtifactToHIU(hiuNotificationRequest, hiuId).block();
    }

    private HIUNotificationRequest hiuNotificationRequest(ConsentArtefactsMessage consentArtefactsMessage) {
        List<ConsentArtefactReference> consentArtefactReferences = consentArtefactReferences(consentArtefactsMessage);
        return HIUNotificationRequest
                .builder()
                .timestamp(consentArtefactsMessage.getTimestamp())
                .requestId(UUID.randomUUID())
                .notification(ConsentNotifier
                        .builder()
                        .consentRequestId(consentArtefactsMessage.getConsentRequestId())
                        .status(consentArtefactsMessage.getStatus())
                        .consentArtefacts(consentArtefactReferences)
                        .build())
                .build();
    }

    private List<ConsentArtefactReference> consentArtefactReferences(ConsentArtefactsMessage consentArtefactsMessage) {
        return consentArtefactsMessage
                .getConsentArtefacts()
                .stream()
                .map(consentArtefact -> ConsentArtefactReference
                        .builder()
                        .id(consentArtefact.getConsentId())
                        .build())
                .collect(Collectors.toList());
    }
}
