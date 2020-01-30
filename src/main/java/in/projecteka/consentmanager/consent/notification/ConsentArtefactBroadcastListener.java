package in.projecteka.consentmanager.consent.notification;

import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.MessageListenerContainerFactory;
import in.projecteka.consentmanager.consent.model.ConsentArtefactsNotification;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;

import static in.projecteka.consentmanager.ConsentManagerConfiguration.HIU_NOTIFICATION_QUEUE;

public class ConsentArtefactBroadcastListener {
    @Autowired
    private MessageListenerContainerFactory messageListenerContainerFactory;
    @Autowired
    private DestinationsConfig destinationsConfig;
    @Autowired
    private Jackson2JsonMessageConverter converter;

    @PostConstruct
    public void subscribe() {
        DestinationsConfig.DestinationInfo destinationInfo = destinationsConfig
                .getQueues()
                .get(HIU_NOTIFICATION_QUEUE);
        if (destinationInfo == null) {
            System.out.println("No queue found by name: " + HIU_NOTIFICATION_QUEUE);
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
