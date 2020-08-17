package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.MessageListenerContainerFactory;
import in.projecteka.consentmanager.clients.ConsentArtefactNotifier;
import in.projecteka.consentmanager.consent.model.ConsentNotificationStatus;
import in.projecteka.consentmanager.consent.model.HIPConsentArtefactRepresentation;
import in.projecteka.consentmanager.consent.model.request.HIPNotificationRequest;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static in.projecteka.consentmanager.Constants.HIP_CONSENT_NOTIFICATION_QUEUE;
import static in.projecteka.consentmanager.consent.model.ConsentStatus.EXPIRED;
import static in.projecteka.consentmanager.consent.model.ConsentStatus.REVOKED;

@AllArgsConstructor
public class HipConsentNotificationListener {
    private static final Logger logger = LoggerFactory.getLogger(HipConsentNotificationListener.class);
    private final MessageListenerContainerFactory messageListenerContainerFactory;
    private final Jackson2JsonMessageConverter converter;
    private final ConsentArtefactNotifier consentArtefactNotifier;
    private final ConsentArtefactRepository consentArtefactRepository;

    @PostConstruct
    public void subscribe() {
        var mlc = messageListenerContainerFactory.createMessageListenerContainer(HIP_CONSENT_NOTIFICATION_QUEUE);

        MessageListener messageListener = message -> {
            try {
                HIPConsentArtefactRepresentation consentArtefact =
                        (HIPConsentArtefactRepresentation) converter.fromMessage(message);
                logger.info("Received notify consent to hip for consent artefact: {}",
                        consentArtefact.getConsentId());

                sendConsentArtefactToHIP(consentArtefact).block();
            } catch (Exception e) {
                throw new AmqpRejectAndDontRequeueException(e.getMessage(), e);
            }
        };
        mlc.setupMessageListener(messageListener);

        mlc.start();
    }


    private Mono<Void> sendConsentArtefactToHIP(HIPConsentArtefactRepresentation consentArtefact) {
        try {
            String hipId = consentArtefact.getConsentDetail().getHip().getId();
            HIPNotificationRequest notificationRequest = hipNotificationRequest(consentArtefact);

            if (consentArtefact.getStatus() == REVOKED) {
                return consentArtefactNotifier.sendConsentArtefactToHIP(notificationRequest, hipId)
                        .then(consentArtefactRepository.saveConsentNotification(
                                consentArtefact.getConsentId(),
                                ConsentNotificationStatus.SENT,
                                ConsentNotificationReceiver.HIP));
            }
            return consentArtefactNotifier.sendConsentArtefactToHIP(notificationRequest, hipId);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Mono.empty();
        }
    }

    private HIPNotificationRequest hipNotificationRequest(HIPConsentArtefactRepresentation consentArtefact) {
        var requestId = UUID.randomUUID();
        var timestamp = LocalDateTime.now(ZoneOffset.UTC);

        if (consentArtefact.getStatus() == EXPIRED || consentArtefact.getStatus() == REVOKED) {
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
