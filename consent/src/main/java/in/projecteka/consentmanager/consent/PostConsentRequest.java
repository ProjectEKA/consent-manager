package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.consent.model.ConsentRequest;
import in.projecteka.library.common.TraceableMessage;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.AmqpTemplate;
import reactor.core.publisher.Mono;

import static in.projecteka.consentmanager.Constants.CONSENT_REQUEST_QUEUE;
import static in.projecteka.library.common.Constants.CORRELATION_ID;


@AllArgsConstructor
public class PostConsentRequest {
    private static final Logger logger = LoggerFactory.getLogger(PostConsentRequest.class);
    private final AmqpTemplate amqpTemplate;
    private final DestinationsConfig destinationsConfig;

    @SneakyThrows
    public Mono<Void> broadcastConsentRequestNotification(ConsentRequest consentRequest) {
        var destinationInfo = destinationsConfig.getQueues().get(CONSENT_REQUEST_QUEUE);
        TraceableMessage traceableMessage = TraceableMessage.builder().correlationId(MDC.get(CORRELATION_ID))
                .message(consentRequest).build();

        return Mono.create(monoSink -> {
            amqpTemplate.convertAndSend(destinationInfo.getExchange(), destinationInfo.getRoutingKey(), traceableMessage);
            logger.info("Broadcasting consent request with request id : {}", consentRequest.getId());
            monoSink.success();
        });
    }
}
