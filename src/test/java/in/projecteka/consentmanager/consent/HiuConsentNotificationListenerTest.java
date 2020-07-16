package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.MessageListenerContainerFactory;
import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.ConsentArtefactNotifier;
import in.projecteka.consentmanager.common.ListenerProperties;
import in.projecteka.consentmanager.consent.model.ConsentArtefactsMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static in.projecteka.consentmanager.ConsentManagerConfiguration.HIU_CONSENT_NOTIFICATION_QUEUE;
import static in.projecteka.consentmanager.consent.model.ConsentStatus.DENIED;
import static in.projecteka.consentmanager.consent.model.ConsentStatus.EXPIRED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;

class HiuConsentNotificationListenerTest {
    @Mock
    private MessageListenerContainerFactory messageListenerContainerFactory;

    @Mock
    private MessageListenerContainer messageListenerContainer;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DestinationsConfig destinationsConfig;

    @Mock
    private DestinationsConfig.DestinationInfo destinationInfo;

    @Mock
    private Jackson2JsonMessageConverter converter;

    @Mock
    private ConsentArtefactNotifier consentArtefactNotifier;

    @Mock
    private AmqpTemplate amqpTemplate;

    @Mock
    private ListenerProperties listenerProperties;

    HiuConsentNotificationListener hiuConsentNotificationListener;

    @BeforeEach
    void init() {
        MockitoAnnotations.initMocks(this);
        hiuConsentNotificationListener = new HiuConsentNotificationListener(
                messageListenerContainerFactory,
                destinationsConfig,
                converter,
                consentArtefactNotifier,
                amqpTemplate,
                listenerProperties
        );
    }

    @Test
    void shouldSendNotificationToHIUWithoutConsentArtefactsOnExpiry() throws ClientError {
        var messageListenerCaptor = ArgumentCaptor.forClass(MessageListener.class);
        var mockMessage = Mockito.mock(Message.class);
        var mockMessageProperties = Mockito.mock(MessageProperties.class);
        ConsentArtefactsMessage consentArtefactMessage = ConsentArtefactsMessage.builder()
                .status(EXPIRED)
                .consentRequestId("CONSENT_ID")
                .timestamp(LocalDateTime.now())
                .hiuId("HIU_ID")
                .build();

        when(destinationsConfig.getQueues().get(HIU_CONSENT_NOTIFICATION_QUEUE)).thenReturn(destinationInfo);
        when(messageListenerContainerFactory
                .createMessageListenerContainer(destinationInfo.getRoutingKey())).thenReturn(messageListenerContainer);
        doNothing().when(messageListenerContainer).setupMessageListener(messageListenerCaptor.capture());
        when(converter.fromMessage(any())).thenReturn(consentArtefactMessage);
        when(consentArtefactNotifier.sendConsentArtifactToHIU(any(), anyString())).thenReturn(Mono.empty());
        when(mockMessage.getMessageProperties()).thenReturn(mockMessageProperties);
        when(mockMessageProperties.getXDeathHeader()).thenReturn(null);

        hiuConsentNotificationListener.subscribe();

        MessageListener messageListener = messageListenerCaptor.getValue();
        messageListener.onMessage(mockMessage);
        verify(messageListenerContainer,times(1)).start();
        verify(messageListenerContainer,times(1))
                .setupMessageListener(messageListenerCaptor.capture());

        verify(consentArtefactNotifier).sendConsentArtifactToHIU(any(), anyString());
    }

    @Test
    void shouldSendNotificationToHIUWithoutConsentArtefactsOnDeny() throws ClientError {
        var messageListenerCaptor = ArgumentCaptor.forClass(MessageListener.class);
        var mockMessage = Mockito.mock(Message.class);
        var mockMessageProperties = Mockito.mock(MessageProperties.class);
        ConsentArtefactsMessage consentArtefactMessage = ConsentArtefactsMessage.builder()
                .status(DENIED)
                .consentRequestId("CONSENT_ID")
                .timestamp(LocalDateTime.now())
                .hiuId("HIU_ID")
                .build();

        when(destinationsConfig.getQueues().get(HIU_CONSENT_NOTIFICATION_QUEUE)).thenReturn(destinationInfo);
        when(messageListenerContainerFactory
                .createMessageListenerContainer(destinationInfo.getRoutingKey())).thenReturn(messageListenerContainer);
        doNothing().when(messageListenerContainer).setupMessageListener(messageListenerCaptor.capture());
        when(converter.fromMessage(any())).thenReturn(consentArtefactMessage);
        when(consentArtefactNotifier.sendConsentArtifactToHIU(any(), anyString())).thenReturn(Mono.empty());
        when(mockMessage.getMessageProperties()).thenReturn(mockMessageProperties);
        when(mockMessageProperties.getXDeathHeader()).thenReturn(null);

        hiuConsentNotificationListener.subscribe();

        MessageListener messageListener = messageListenerCaptor.getValue();
        messageListener.onMessage(mockMessage);
        verify(messageListenerContainer,times(1)).start();
        verify(messageListenerContainer,times(1))
                .setupMessageListener(messageListenerCaptor.capture());

        verify(consentArtefactNotifier).sendConsentArtifactToHIU(any(), anyString());
    }


}