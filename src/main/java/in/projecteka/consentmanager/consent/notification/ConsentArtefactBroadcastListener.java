package in.projecteka.consentmanager.consent.notification;

import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.MessageListenerContainerFactory;
import in.projecteka.consentmanager.consent.model.ConsentArtefactsNotification;
import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import javax.annotation.PostConstruct;

import static in.projecteka.consentmanager.ConsentManagerConfiguration.CONSENT_GRANTED_QUEUE;

@AllArgsConstructor
public class ConsentArtefactBroadcastListener {
    private MessageListenerContainerFactory messageListenerContainerFactory;
    private DestinationsConfig destinationsConfig;
    private Jackson2JsonMessageConverter converter;

    @PostConstruct
    public void subscribe() {
        DestinationsConfig.DestinationInfo destinationInfo = destinationsConfig
                .getQueues()
                .get(CONSENT_GRANTED_QUEUE);
        if (destinationInfo == null) {
            System.out.println("No queue found by name: " + CONSENT_GRANTED_QUEUE);
            return;
        }

        MessageListenerContainer mlc = messageListenerContainerFactory
                .createMessageListenerContainer(destinationInfo.getRoutingKey());

        mlc.setupMessageListener(message -> {
            ConsentArtefactsNotification consentArtefactsNotification = (ConsentArtefactsNotification) converter.fromMessage(message);
            System.out.println("Hiu " + consentArtefactsNotification.getCallBackUrl());
            // TODO: call hiu here
        });

        mlc.start();
    }
}
