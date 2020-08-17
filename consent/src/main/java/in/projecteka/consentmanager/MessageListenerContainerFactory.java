package in.projecteka.consentmanager;

import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

@AllArgsConstructor
public class MessageListenerContainerFactory {
    private final ConnectionFactory connectionFactory;
    private final Jackson2JsonMessageConverter converter;

    public MessageListenerContainer createMessageListenerContainer(String queueName) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.addQueueNames(queueName);

        MessageListenerAdapter messageListenerAdapter = new MessageListenerAdapter();
        messageListenerAdapter.setMessageConverter(converter);
        container.setMessageListener(messageListenerAdapter);
        return container;
    }
}