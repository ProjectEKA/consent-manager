package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.consent.model.ConsentRequest;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import reactor.core.publisher.Mono;

import static in.projecteka.consentmanager.common.Constants.CONSENT_REQUEST_QUEUE;


@AllArgsConstructor
public class PostConsentRequest {
    private static final Logger logger = LoggerFactory.getLogger(PostConsentRequest.class);
    private final AmqpTemplate amqpTemplate;
    private final DestinationsConfig destinationsConfig;

    @SneakyThrows
    public Mono<Void> broadcastConsentRequestNotification(ConsentRequest consentRequest) {
        var destinationInfo = destinationsConfig.getQueues().get(CONSENT_REQUEST_QUEUE);

        return Mono.create(monoSink -> {
            amqpTemplate.convertAndSend(destinationInfo.getExchange(), destinationInfo.getRoutingKey(), consentRequest);
            logger.info("Broadcasting consent request with request id : {}", consentRequest.getId());
            monoSink.success();
        });
    }
}
