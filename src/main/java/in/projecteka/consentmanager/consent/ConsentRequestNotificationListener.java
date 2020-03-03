package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.MessageListenerContainerFactory;
import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.ConsentNotificationClient;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.consent.model.*;
import lombok.AllArgsConstructor;
import org.apache.log4j.Logger;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.stream.Collectors;

import static in.projecteka.consentmanager.ConsentManagerConfiguration.CONSENT_REQUEST_QUEUE;
import static in.projecteka.consentmanager.clients.ClientError.queueNotFound;

@AllArgsConstructor
public class ConsentRequestNotificationListener {
    private static final Logger logger = Logger.getLogger(ConsentArtefactBroadcastListener.class);
    private MessageListenerContainerFactory messageListenerContainerFactory;
    private DestinationsConfig destinationsConfig;
    private Jackson2JsonMessageConverter converter;
    private ConsentNotificationClient consentNotificationClient;
    private UserServiceClient userServiceClient;
    private ConsentServiceProperties consentServiceProperties;

    @PostConstruct
    public void subscribe() throws ClientError {
        DestinationsConfig.DestinationInfo destinationInfo = destinationsConfig
                .getQueues()
                .get(CONSENT_REQUEST_QUEUE);
        if (destinationInfo == null) {
            logger.error(CONSENT_REQUEST_QUEUE + " not found");
            throw queueNotFound();
        }

        MessageListenerContainer mlc = messageListenerContainerFactory
                .createMessageListenerContainer(destinationInfo.getRoutingKey());

        MessageListener messageListener = message -> {
            ConsentRequest consentRequest =
                    (ConsentRequest) converter.fromMessage(message);
            logger.info(String.format(
                    "Received message for Request id : %s", consentRequest.getRequestId()));
            createNotificationMessage(consentRequest)
                    .flatMap(this::callNotificationService)
                    .block();
        };
        mlc.setupMessageListener(messageListener);
        mlc.start();
    }

    public Mono<Void> callNotificationService(NotificationMessage notificationMessage) {
        return consentNotificationClient.sendToNotificationService(notificationMessage);
    }

    private Mono<NotificationMessage> createNotificationMessage(ConsentRequest consentRequest) {
        return userServiceClient.userOf(consentRequest.getRequestedDetail().getPatient().getId())
                .flatMap(user -> Mono.just(NotificationMessage.builder()
                        .communication(Communication.builder()
                                .communicationType(CommunicationType.MOBILE)
                                .value(user.getPhone())
                                .build())
                        .id(consentRequest.getRequestId())
                        .notificationAction(NotificationAction.CONSENT_REQUEST_CREATED)
                        .notificationContent(NotificationContent.builder()
                                .requester(consentRequest.getRequestedDetail().getRequester().getName())
                                .consentRequestId(consentRequest.getRequestId())
                                .hiTypes(Arrays.stream(consentRequest.getRequestedDetail().getHiTypes())
                                        .map(HIType::getValue)
                                        .collect(Collectors.joining(",")))
                                .deepLinkUrl(String.format("%s/consent/%s",
                                        consentServiceProperties.getConsentServiceUrl(),
                                        consentRequest.getRequestId()))
                                .build())
                        .build()));
    }
}
