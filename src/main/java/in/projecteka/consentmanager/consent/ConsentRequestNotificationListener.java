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
            ConsentRequest consentRequest = (ConsentRequest) converter.fromMessage(message);
            logger.info(String.format("Received message for Request id : %s", consentRequest.getId()));
            createNotificationMessage(consentRequest)
                    .flatMap(this::NotifyUserWith)
                    .block();
        };
        mlc.setupMessageListener(messageListener);
        mlc.start();
    }

    public Mono<Void> NotifyUserWith(Notification notification) {
        return consentNotificationClient.send(notification);
    }

    private Mono<Notification> createNotificationMessage(ConsentRequest consentRequest) {
        return userServiceClient.userOf(consentRequest.getDetail().getPatient().getId())
                .map(user -> Notification.builder()
                        .communication(Communication.builder()
                                .communicationType(CommunicationType.MOBILE)
                                .value(user.getPhone())
                                .build())
                        .id(consentRequest.getId())
                        .action(Action.CONSENT_REQUEST_CREATED)
                        .content(Content.builder()
                                .requester(consentRequest.getDetail().getRequester().getName())
                                .consentRequestId(consentRequest.getId())
                                .hiTypes(Arrays.stream(consentRequest.getDetail().getHiTypes())
                                        .map(HIType::getValue)
                                        .collect(Collectors.joining(",")))
                                .deepLinkUrl(String.format("%s/consent/%s",
                                        consentServiceProperties.getUrl(),
                                        consentRequest.getId()))
                                .build())
                        .build());
    }
}
