package in.projecteka.consentmanager.consent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import in.projecteka.consentmanager.MessageListenerContainerFactory;
import in.projecteka.consentmanager.clients.ConsentArtefactNotifier;
import in.projecteka.consentmanager.consent.model.ConsentNotificationStatus;
import in.projecteka.consentmanager.consent.model.HIPConsentArtefactRepresentation;
import in.projecteka.consentmanager.consent.model.request.HIPNotificationRequest;
import in.projecteka.library.common.TraceableMessage;
import in.projecteka.library.common.cache.CacheAdapter;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static in.projecteka.consentmanager.Constants.HIP_CONSENT_NOTIFICATION_QUEUE;
import static in.projecteka.consentmanager.consent.model.ConsentStatus.EXPIRED;
import static in.projecteka.consentmanager.consent.model.ConsentStatus.GRANTED;
import static in.projecteka.consentmanager.consent.model.ConsentStatus.REVOKED;
import static in.projecteka.consentmanager.consent.model.HipConsentArtefactNotificationStatus.NOTIFYING;
import static in.projecteka.library.common.Constants.CORRELATION_ID;

@AllArgsConstructor
public class HipConsentNotificationListener {
    private static final Logger logger = LoggerFactory.getLogger(HipConsentNotificationListener.class);
    private final MessageListenerContainerFactory messageListenerContainerFactory;
    private final Jackson2JsonMessageConverter converter;
    private final ConsentArtefactNotifier consentArtefactNotifier;
    private final ConsentArtefactRepository consentArtefactRepository;
    private final CacheAdapter<String, String> cache;

    @PostConstruct
    public void subscribe() {
        var mlc = messageListenerContainerFactory.createMessageListenerContainer(HIP_CONSENT_NOTIFICATION_QUEUE);

        MessageListener messageListener = message -> {
            try {
                TraceableMessage traceableMessage = (TraceableMessage) converter.fromMessage(message);
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());
                HIPConsentArtefactRepresentation consentArtefact = mapper.convertValue(traceableMessage.getMessage()
                        , HIPConsentArtefactRepresentation.class);
                MDC.put(CORRELATION_ID, traceableMessage.getCorrelationId());
                logger.info("Received notify consent to hip for consent artefact: {}",
                        consentArtefact.getConsentId());

                sendConsentArtefactToHIP(consentArtefact)
                        .subscriberContext(ctx -> {
                            Optional<String> correlationId = Optional.ofNullable(MDC.get(CORRELATION_ID));
                            return correlationId.map(id -> ctx.put(CORRELATION_ID, id))
                                    .orElseGet(() -> ctx.put(CORRELATION_ID, UUID.randomUUID().toString()));
                        })
                        .block();
                MDC.clear();
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
            var artefactPublisher = Mono.defer(() -> consentArtefactNotifier.sendConsentArtefactToHIP(notificationRequest, hipId));
            if (consentArtefact.getStatus() == GRANTED) {
                return cache.put(consentArtefact.getConsentId(), NOTIFYING.toString())
                        .then(artefactPublisher);
            }
            return artefactPublisher;
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
