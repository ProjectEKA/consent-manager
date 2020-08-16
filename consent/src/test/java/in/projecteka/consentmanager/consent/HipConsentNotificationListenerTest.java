package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.MessageListenerContainerFactory;
import in.projecteka.consentmanager.clients.ConsentArtefactNotifier;
import in.projecteka.consentmanager.consent.model.ConsentNotificationStatus;
import in.projecteka.consentmanager.consent.model.HIPConsentArtefact;
import in.projecteka.consentmanager.consent.model.HIPConsentArtefactRepresentation;
import in.projecteka.consentmanager.consent.model.HIPReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import reactor.core.publisher.Mono;

import static in.projecteka.consentmanager.common.Constants.HIP_CONSENT_NOTIFICATION_QUEUE;
import static in.projecteka.consentmanager.consent.model.ConsentStatus.EXPIRED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HipConsentNotificationListenerTest {
    @Mock
    private MessageListenerContainerFactory messageListenerContainerFactory;

    @Mock
    private MessageListenerContainer messageListenerContainer;

    @Mock
    private Jackson2JsonMessageConverter converter;

    @Mock
    private ConsentArtefactNotifier consentArtefactNotifier;

    @Mock
    private ConsentArtefactRepository consentArtefactRepository;

    HipConsentNotificationListener hipConsentNotificationListener;

    @BeforeEach
    void init() {
        MockitoAnnotations.initMocks(this);
        hipConsentNotificationListener = new HipConsentNotificationListener(
                messageListenerContainerFactory,
                converter,
                consentArtefactNotifier,
                consentArtefactRepository);
    }

    @Test
    void shouldSendNotificationToHIPWithoutConsentArtefactsOnExpiry() {
        var messageListenerCaptor = ArgumentCaptor.forClass(MessageListener.class);
        var mockMessage = Mockito.mock(Message.class);
        var mockMessageProperties = Mockito.mock(MessageProperties.class);
        var consentId = "Consent_id";
        HIPConsentArtefactRepresentation hipConsentArtefactRepresentation = HIPConsentArtefactRepresentation.builder()
                .status(EXPIRED)
                .consentDetail(HIPConsentArtefact.builder()
                        .hip(HIPReference.builder()
                                .id("HIP_ID")
                                .build())
                        .build())
                .consentId(consentId)
                .build();
        when(messageListenerContainerFactory
                .createMessageListenerContainer(HIP_CONSENT_NOTIFICATION_QUEUE)).thenReturn(messageListenerContainer);
        doNothing().when(messageListenerContainer).setupMessageListener(messageListenerCaptor.capture());
        when(converter.fromMessage(any())).thenReturn(hipConsentArtefactRepresentation);
        when(consentArtefactNotifier.sendConsentArtefactToHIP(any(), anyString())).thenReturn(Mono.empty());
        when(consentArtefactRepository.saveConsentNotification(consentId, ConsentNotificationStatus.SENT, ConsentNotificationReceiver.HIP)).thenReturn(Mono.empty());
        when(mockMessage.getMessageProperties()).thenReturn(mockMessageProperties);
        when(mockMessageProperties.getXDeathHeader()).thenReturn(null);

        hipConsentNotificationListener.subscribe();

        MessageListener messageListener = messageListenerCaptor.getValue();
        messageListener.onMessage(mockMessage);
        verify(messageListenerContainer, times(1)).start();
        verify(messageListenerContainer, times(1))
                .setupMessageListener(messageListenerCaptor.capture());

        verify(consentArtefactNotifier).sendConsentArtefactToHIP(any(), anyString());
    }
}
