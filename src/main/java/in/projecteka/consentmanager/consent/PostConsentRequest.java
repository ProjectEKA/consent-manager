package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.consent.model.ConsentRequest;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.amqp.core.AmqpTemplate;
import reactor.core.publisher.Mono;

import static in.projecteka.consentmanager.ConsentManagerConfiguration.CONSENT_REQUEST_QUEUE;
import static in.projecteka.consentmanager.clients.ClientError.queueNotFound;

@AllArgsConstructor
public class PostConsentRequest {
    private static final Logger logger = LoggerFactory.getLogger(PostConsentRequest.class);
    private final AmqpTemplate amqpTemplate;
    private final DestinationsConfig destinationsConfig;

    @SneakyThrows
    public Mono<Void> broadcastConsentRequestNotification(ConsentRequest consentRequest) {
        DestinationsConfig.DestinationInfo destinationInfo =
                destinationsConfig.getQueues().get(CONSENT_REQUEST_QUEUE);
        if (destinationInfo == null) {
            logger.info(CONSENT_REQUEST_QUEUE + " not found");
            throw queueNotFound();
        }
        return Mono.create(monoSink -> {
            amqpTemplate.convertAndSend(destinationInfo.getExchange(), destinationInfo.getRoutingKey(), consentRequest);
            logger.info("Broadcasting consent request with request id : {}", consentRequest.getId());
            monoSink.success();
        });
    }
}
