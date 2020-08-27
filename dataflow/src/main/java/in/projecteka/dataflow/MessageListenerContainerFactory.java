package in.projecteka.dataflow;

//@AllArgsConstructor
//public class MessageListenerContainerFactory {
//    private final ConnectionFactory connectionFactory;
//    private final Jackson2JsonMessageConverter converter;
//
//    public MessageListenerContainer createMessageListenerContainer(String queueName) {
//        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
//        container.addQueueNames(queueName);
//
//        MessageListenerAdapter messageListenerAdapter = new MessageListenerAdapter();
//        messageListenerAdapter.setMessageConverter(converter);
//        container.setMessageListener(messageListenerAdapter);
//        return container;
//    }
//}