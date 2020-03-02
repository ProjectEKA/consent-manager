package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.consent.model.ConsentRequest;
import in.projecteka.consentmanager.dataflow.PostDataFlowRequestApproval;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.log4j.Logger;
import org.springframework.amqp.core.AmqpTemplate;
import reactor.core.publisher.Mono;

import static in.projecteka.consentmanager.ConsentManagerConfiguration.CONSENT_REQUEST_QUEUE;
import static in.projecteka.consentmanager.clients.ClientError.queueNotFound;

@AllArgsConstructor
public class PostConsentRequestNotification {
    private static final Logger logger = Logger.getLogger(PostDataFlowRequestApproval.class);
    private AmqpTemplate amqpTemplate;
    private DestinationsConfig destinationsConfig;

    @SneakyThrows
    public Mono<Void> broadcastConsentRequestNotification(ConsentRequest consentRequest) {
        DestinationsConfig.DestinationInfo destinationInfo =
                destinationsConfig.getQueues().get(CONSENT_REQUEST_QUEUE);
        if (destinationInfo == null) {
            logger.info(CONSENT_REQUEST_QUEUE + " not found");
            throw queueNotFound();
        }
        return Mono.create(monoSink -> {
            amqpTemplate.convertAndSend(
                    destinationInfo.getExchange(),
                    destinationInfo.getRoutingKey(),
                    consentRequest);
            logger.info("Broadcasting consent request with request id : " + consentRequest.getRequestId());
            monoSink.success();
        });
    }
}
