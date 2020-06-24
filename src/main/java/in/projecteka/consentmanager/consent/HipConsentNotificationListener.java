package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.MessageListenerContainerFactory;
import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.ConsentArtefactNotifier;
import in.projecteka.consentmanager.common.CentralRegistry;
import in.projecteka.consentmanager.consent.model.HIPConsentArtefactRepresentation;
import in.projecteka.consentmanager.consent.model.request.HIPNotificationRequest;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;

import java.time.LocalDateTime;
import java.util.UUID;

import static in.projecteka.consentmanager.ConsentManagerConfiguration.HIP_CONSENT_NOTIFICATION_QUEUE;
import static in.projecteka.consentmanager.consent.model.ConsentStatus.EXPIRED;

@AllArgsConstructor
public class HipConsentNotificationListener {
    private static final Logger logger = LoggerFactory.getLogger(HipConsentNotificationListener.class);
    private final MessageListenerContainerFactory messageListenerContainerFactory;
    private final DestinationsConfig destinationsConfig;
    private final Jackson2JsonMessageConverter converter;
    private final ConsentArtefactNotifier consentArtefactNotifier;
    private final CentralRegistry centralRegistry;

    @PostConstruct
    public void subscribe() throws ClientError {
        DestinationsConfig.DestinationInfo destinationInfo = destinationsConfig
                .getQueues()
                .get(HIP_CONSENT_NOTIFICATION_QUEUE);

        MessageListenerContainer mlc = messageListenerContainerFactory
                .createMessageListenerContainer(destinationInfo.getRoutingKey());

        MessageListener messageListener = message -> {
            try {
                HIPConsentArtefactRepresentation consentArtefact =
                        (HIPConsentArtefactRepresentation) converter.fromMessage(message);
                logger.info("Received notify consent to hip for consent artefact: {}",
                        consentArtefact.getConsentId());

                sendConsentArtefactToHIP(consentArtefact).block();
            } catch (Exception e) {
                throw new AmqpRejectAndDontRequeueException(e.getMessage(),e);
            }
        };
        mlc.setupMessageListener(messageListener);

        mlc.start();
    }


    private Mono<Void> sendConsentArtefactToHIP(HIPConsentArtefactRepresentation consentArtefact) {
        String hipId = consentArtefact.getConsentDetail().getHip().getId();
        HIPNotificationRequest notificationRequest = hipNotificationRequest(consentArtefact);

        return consentArtefactNotifier.sendConsentArtefactToHIP(notificationRequest, hipId);
    }

    private HIPNotificationRequest hipNotificationRequest(HIPConsentArtefactRepresentation consentArtefact) {
        var requestId = UUID.randomUUID();
        var timestamp = LocalDateTime.now();

        if (consentArtefact.getStatus() == EXPIRED) {
            return HIPNotificationRequest.builder()
                    .requestId(requestId)
                    .timestamp(timestamp)
                    .notification(HIPConsentArtefactRepresentation.builder()
                            .status(consentArtefact.getStatus())
                            .consentId(consentArtefact.getConsentId())
                            .build())
                    .build();
        }
        return HIPNotificationRequest.builder()
                .notification(consentArtefact)
                .requestId(requestId)
                .timestamp(timestamp)
                .build();
    }
}
