package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.MessageListenerContainerFactory;
import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.OtpServiceClient;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.consent.model.Action;
import in.projecteka.consentmanager.consent.model.Communication;
import in.projecteka.consentmanager.consent.model.CommunicationType;
import in.projecteka.consentmanager.consent.model.ConsentRequest;
import in.projecteka.consentmanager.consent.model.Content;
import in.projecteka.consentmanager.consent.model.HIType;
import in.projecteka.consentmanager.consent.model.Notification;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.stream.Collectors;

import static in.projecteka.consentmanager.ConsentManagerConfiguration.CONSENT_REQUEST_QUEUE;

@AllArgsConstructor
public class ConsentRequestNotificationListener {
    private static final Logger logger = LoggerFactory.getLogger(ConsentRequestNotificationListener.class);
    private final MessageListenerContainerFactory messageListenerContainerFactory;
    private final DestinationsConfig destinationsConfig;
    private final Jackson2JsonMessageConverter converter;
    private final OtpServiceClient consentNotificationClient;
    private final UserServiceClient userServiceClient;
    private final ConsentServiceProperties consentServiceProperties;

    @PostConstruct
    public void subscribe() throws ClientError {
        DestinationsConfig.DestinationInfo destinationInfo = destinationsConfig
                .getQueues()
                .get(CONSENT_REQUEST_QUEUE);

        MessageListenerContainer mlc = messageListenerContainerFactory
                .createMessageListenerContainer(destinationInfo.getRoutingKey());

        MessageListener messageListener = message -> {
            try {
                ConsentRequest consentRequest = (ConsentRequest) converter.fromMessage(message);
                logger.info("Received message for Request id : {}", consentRequest.getId());
                createNotificationMessage(consentRequest)
                        .flatMap(this::notifyUserWith)
                        .block();
            } catch (Exception e) {
                throw new AmqpRejectAndDontRequeueException(e.getMessage(),e);
            }
        };
        mlc.setupMessageListener(messageListener);
        mlc.start();
    }

    public Mono<Void> notifyUserWith(Notification notification) {
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
