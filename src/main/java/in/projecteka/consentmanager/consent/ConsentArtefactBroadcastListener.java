package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.MessageListenerContainerFactory;
import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.ConsentArtefactNotifier;
import in.projecteka.consentmanager.consent.model.ConsentArtefactsNotificationMessage;
import in.projecteka.consentmanager.consent.model.ConsentStatus;
import in.projecteka.consentmanager.consent.model.request.HIUNotificationRequest;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactReference;
import lombok.AllArgsConstructor;
import org.apache.log4j.Logger;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import javax.annotation.PostConstruct;

import java.util.List;
import java.util.stream.Collectors;

import static in.projecteka.consentmanager.ConsentManagerConfiguration.CONSENT_GRANTED_QUEUE;
import static in.projecteka.consentmanager.clients.ClientError.queueNotFound;

@AllArgsConstructor
public class ConsentArtefactBroadcastListener {
    private static final Logger logger = Logger.getLogger(ConsentArtefactBroadcastListener.class);
    private MessageListenerContainerFactory messageListenerContainerFactory;
    private DestinationsConfig destinationsConfig;
    private Jackson2JsonMessageConverter converter;
    private ConsentArtefactNotifier consentArtefactNotifier;

    @PostConstruct
    public void subscribe() throws ClientError {
        DestinationsConfig.DestinationInfo destinationInfo = destinationsConfig
                .getQueues()
                .get(CONSENT_GRANTED_QUEUE);
        if (destinationInfo == null) {
            logger.error(CONSENT_GRANTED_QUEUE + " not found");
            throw queueNotFound();
        }

        MessageListenerContainer mlc = messageListenerContainerFactory
                .createMessageListenerContainer(destinationInfo.getRoutingKey());

        MessageListener messageListener = message -> {
            ConsentArtefactsNotificationMessage consentArtefactsNotificationMessage =
                    (ConsentArtefactsNotificationMessage) converter.fromMessage(message);
            logger.info("Received message for Request id : " + consentArtefactsNotificationMessage
                    .getRequestId());

            notifyHiu(consentArtefactsNotificationMessage);
        };
        mlc.setupMessageListener(messageListener);

        mlc.start();
    }

    private void notifyHiu(ConsentArtefactsNotificationMessage consentArtefactsNotificationMessage) {
        consentArtefactNotifier.notifyHiu(hiuNotificationRequest(consentArtefactsNotificationMessage,
                consentArtefactsNotificationMessage.getRequestId()),
                consentArtefactsNotificationMessage.getHiuCallBackUrl())
                .block();
    }

    private HIUNotificationRequest hiuNotificationRequest(
            ConsentArtefactsNotificationMessage consentArtefactsNotificationMessage,
            String requestId) {
        List<ConsentArtefactReference> consentArtefactReferences = consentArtefactsNotificationMessage
                .getConsentArtefacts()
                .stream()
                .map(consentArtefact -> ConsentArtefactReference
                        .builder()
                        .status(ConsentStatus.GRANTED)
                        .id(consentArtefact.getConsentId())
                        .build())
                .collect(Collectors.toList());

        return HIUNotificationRequest
                .builder()
                .consents(consentArtefactReferences)
                .consentRequestId(requestId)
                .build();
    }
}
