package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.consent.model.ConsentArtefactsMessage;
import in.projecteka.consentmanager.consent.model.ConsentRequestDetail;
import in.projecteka.consentmanager.consent.model.ConsentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static in.projecteka.consentmanager.consent.TestBuilders.consentRequestDetail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class ConsentRequestSchedulerTest {

    @Mock
    private ConsentRequestRepository consentRequestRepository;

    @Mock
    private ConsentServiceProperties consentServiceProperties;

    @Mock
    private ConsentNotificationPublisher consentNotificationPublisher;

    private ConsentRequestScheduler consentRequestScheduler;

    @BeforeEach
    public void setUp() {
        initMocks(this);
        consentRequestScheduler = new ConsentRequestScheduler(consentRequestRepository,
                consentServiceProperties,
                consentNotificationPublisher);
    }

    @Test
    public void processConsentRequestsWhenNoneOfTheRequestAreInRequestedState() {
        when(consentRequestRepository.getConsentsByStatus(ConsentStatus.REQUESTED)).thenReturn(Flux.empty());

        consentRequestScheduler.processExpiredConsentRequests();

        verify(consentRequestRepository, times(1)).getConsentsByStatus(ConsentStatus.REQUESTED);
    }

    @Test
    public void processConsentRequestsWhenRequestIsNotExpired() {
        ConsentRequestDetail consentRequestDetail =
                consentRequestDetail().status(ConsentStatus.REQUESTED).createdAt(LocalDateTime.now()).build();
        when(consentRequestRepository.getConsentsByStatus(ConsentStatus.REQUESTED)).thenReturn(Flux.just(consentRequestDetail));
        when(consentServiceProperties.getConsentRequestExpiry()).thenReturn(10);

        consentRequestScheduler.processExpiredConsentRequests();

        verify(consentRequestRepository, times(1)).getConsentsByStatus(ConsentStatus.REQUESTED);
        verify(consentServiceProperties, times(1)).getConsentRequestExpiry();
    }

    @Test
    public void processConsentRequestsWhenRequestIsExpired() {
        LocalDateTime createdAt = LocalDateTime.now().minus(Duration.ofMinutes(20));
        var consentRequestDetail =
                consentRequestDetail().status(ConsentStatus.REQUESTED).createdAt(createdAt).build();
        var consentArtefactsMessage = ConsentArtefactsMessage.builder()
                .status(ConsentStatus.EXPIRED)
                .timestamp(consentRequestDetail.getLastUpdated())
                .consentRequestId(consentRequestDetail.getRequestId())
                .consentArtefacts(List.of())
                .hiuId(consentRequestDetail.getHiu().getId())
                .build();
        when(consentRequestRepository.getConsentsByStatus(ConsentStatus.REQUESTED))
                .thenReturn(Flux.just(consentRequestDetail));
        when(consentServiceProperties.getConsentRequestExpiry()).thenReturn(1);
        when(consentRequestRepository.updateStatus(consentRequestDetail.getRequestId(), ConsentStatus.EXPIRED))
                .thenReturn(Mono.empty());
        when(consentNotificationPublisher.publish(consentArtefactsMessage)).thenReturn(Mono.empty());

        consentRequestScheduler.processExpiredConsentRequests();

        verify(consentRequestRepository, times(1)).getConsentsByStatus(ConsentStatus.REQUESTED);
        verify(consentServiceProperties, times(1)).getConsentRequestExpiry();
        verify(consentRequestRepository, times(1))
                .updateStatus(consentRequestDetail.getRequestId(), ConsentStatus.EXPIRED);
        verify(consentNotificationPublisher, times(1)).publish(consentArtefactsMessage);

    }
}