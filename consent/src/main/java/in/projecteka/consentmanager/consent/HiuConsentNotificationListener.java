package in.projecteka.consentmanager.consent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import in.projecteka.consentmanager.MessageListenerContainerFactory;
import in.projecteka.consentmanager.clients.ConsentArtefactNotifier;
import in.projecteka.consentmanager.consent.model.ConsentArtefactsMessage;
import in.projecteka.consentmanager.consent.model.request.ConsentArtefactReference;
import in.projecteka.consentmanager.consent.model.request.ConsentNotifier;
import in.projecteka.consentmanager.consent.model.request.HIUNotificationRequest;
import in.projecteka.consentmanager.properties.ListenerProperties;
import in.projecteka.library.common.TraceableMessage;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static in.projecteka.consentmanager.Constants.HIU_CONSENT_NOTIFICATION_QUEUE;
import static in.projecteka.consentmanager.Constants.PARKING_EXCHANGE;
import static in.projecteka.library.common.Constants.CORRELATION_ID;

@AllArgsConstructor
public class HiuConsentNotificationListener {
    private static final Logger logger = LoggerFactory.getLogger(HiuConsentNotificationListener.class);
    private final MessageListenerContainerFactory messageListenerContainerFactory;
    private final Jackson2JsonMessageConverter converter;
    private final ConsentArtefactNotifier consentArtefactNotifier;
    private final AmqpTemplate amqpTemplate;
    private final ListenerProperties listenerProperties;

    @PostConstruct
    public void subscribe() {

        var mlc = messageListenerContainerFactory.createMessageListenerContainer(HIU_CONSENT_NOTIFICATION_QUEUE);

        MessageListener messageListener = message -> {
            try {
                //This is NOT a generic solution. Based on the context, it either needs to retry, or it might also need to propagate the error to the upstream systems.
                //TODO be revisited during Gateway development
                if (hasExceededRetryCount(message)) {
                    amqpTemplate.convertAndSend(PARKING_EXCHANGE,
                            message.getMessageProperties().getReceivedRoutingKey(),
                            message);
                    return;
                }
                TraceableMessage traceableMessage = (TraceableMessage) converter.fromMessage(message);
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());
                ConsentArtefactsMessage consentArtefactsMessage = mapper.convertValue(traceableMessage.getMessage(),
                        ConsentArtefactsMessage.class);
                MDC.put(CORRELATION_ID, traceableMessage.getCorrelationId());
                logger.info("Received message for Request id : {}", consentArtefactsMessage.getConsentRequestId());
                notifyHiu(consentArtefactsMessage);
                MDC.clear();
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
        consentArtefactNotifier.sendConsentArtifactToHIU(hiuNotificationRequest, hiuId)
                .subscriberContext(ctx -> {
                    Optional<String> correlationId = Optional.ofNullable(MDC.get(CORRELATION_ID));
                    return correlationId.map(id -> ctx.put(CORRELATION_ID, id))
                            .orElseGet(() -> ctx.put(CORRELATION_ID, UUID.randomUUID().toString()));
                }).block();
    }

    private HIUNotificationRequest hiuNotificationRequest(ConsentArtefactsMessage consentArtefactsMessage) {
        List<ConsentArtefactReference> consentArtefactReferences = consentArtefactReferences(consentArtefactsMessage);
        return HIUNotificationRequest
                .builder()
                .timestamp(LocalDateTime.now(ZoneOffset.UTC))
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
