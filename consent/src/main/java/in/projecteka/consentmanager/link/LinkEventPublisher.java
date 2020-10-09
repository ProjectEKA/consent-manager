package in.projecteka.consentmanager.link;

import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.library.common.TraceableMessage;
import in.projecteka.consentmanager.link.link.model.CCLinkEvent;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.AmqpTemplate;
import reactor.core.publisher.Mono;

import static in.projecteka.consentmanager.Constants.HIP_LINK_QUEUE;
import static in.projecteka.library.common.Constants.CORRELATION_ID;


@AllArgsConstructor
public class LinkEventPublisher {
    private final AmqpTemplate amqpTemplate;
    private final DestinationsConfig destinationsConfig;

    private static final Logger logger = LoggerFactory.getLogger(LinkEventPublisher.class);

    public Mono<Void> publish(CCLinkEvent message) {
        return Mono.create(monoSink -> {
            broadcastLinkEvent(message);
            monoSink.success();
        });
    }

    private void broadcastLinkEvent(CCLinkEvent message) {
        DestinationsConfig.DestinationInfo destinationInfo = destinationsConfig.getQueues()
                .get(HIP_LINK_QUEUE);
        TraceableMessage traceableMessage = TraceableMessage.builder()
                .correlationId(MDC.get(CORRELATION_ID))
                .message(message)
                .build();
        logger.debug("Raising LINK Event: " +
                        "exchange: {} , routing key: {} , " +
                        "correlation id: {}, " +
                        "patient: {}, hip: {}",
                        destinationInfo.getExchange(), destinationInfo.getRoutingKey(),
                        traceableMessage.getCorrelationId(),
                        message.getHealthNumber(), message.getHipId());
        amqpTemplate.convertAndSend(destinationInfo.getExchange(),
                destinationInfo.getRoutingKey(), traceableMessage);
        logger.info("Raised LINK Event: correlation Id: {}",  traceableMessage.getCorrelationId());
    }
}
