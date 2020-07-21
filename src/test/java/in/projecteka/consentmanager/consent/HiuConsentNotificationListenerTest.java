package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.MessageListenerContainerFactory;
import in.projecteka.consentmanager.clients.ConsentArtefactNotifier;
import in.projecteka.consentmanager.common.ListenerProperties;
import in.projecteka.consentmanager.consent.model.ConsentArtefactsMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import java.util.List;

import static in.projecteka.consentmanager.common.Constants.HIU_CONSENT_NOTIFICATION_QUEUE;
import static in.projecteka.consentmanager.consent.model.ConsentStatus.DENIED;
import static in.projecteka.consentmanager.consent.model.ConsentStatus.EXPIRED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HiuConsentNotificationListenerTest {
    @Mock
    private MessageListenerContainerFactory messageListenerContainerFactory;

    @Mock
    private MessageListenerContainer messageListenerContainer;

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
                converter,
                consentArtefactNotifier,
                amqpTemplate,
                listenerProperties
        );
    }

    @Test
    void shouldSendNotificationToHIUWithoutConsentArtefactsOnExpiry() {
        var messageListenerCaptor = ArgumentCaptor.forClass(MessageListener.class);
        var mockMessage = Mockito.mock(Message.class);
        var mockMessageProperties = Mockito.mock(MessageProperties.class);
        ConsentArtefactsMessage consentArtefactMessage = ConsentArtefactsMessage.builder()
                .status(EXPIRED)
                .consentRequestId("CONSENT_ID")
                .timestamp(LocalDateTime.now())
                .hiuId("HIU_ID")
                .consentArtefacts(List.of())
                .build();
        when(messageListenerContainerFactory
                .createMessageListenerContainer(HIU_CONSENT_NOTIFICATION_QUEUE)).thenReturn(messageListenerContainer);
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
    void shouldSendNotificationToHIUWithoutConsentArtefactsOnDeny() {
        var messageListenerCaptor = ArgumentCaptor.forClass(MessageListener.class);
        var mockMessage = Mockito.mock(Message.class);
        var mockMessageProperties = Mockito.mock(MessageProperties.class);
        ConsentArtefactsMessage consentArtefactMessage = ConsentArtefactsMessage.builder()
                .status(DENIED)
                .consentRequestId("CONSENT_ID")
                .timestamp(LocalDateTime.now())
                .hiuId("HIU_ID")
                .consentArtefacts(List.of())
                .build();
        when(messageListenerContainerFactory
                .createMessageListenerContainer(HIU_CONSENT_NOTIFICATION_QUEUE)).thenReturn(messageListenerContainer);
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