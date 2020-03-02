package in.projecteka.consentmanager.consent.service;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.ClientRegistryClient;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.clients.model.Provider;
import in.projecteka.consentmanager.clients.model.User;
import in.projecteka.consentmanager.consent.ConsentManager;
import in.projecteka.consentmanager.consent.ConsentRequestNotificationListener;
import in.projecteka.consentmanager.consent.PostConsentApproval;
import in.projecteka.consentmanager.consent.PostConsentRequestNotification;
import in.projecteka.consentmanager.consent.model.ConsentRequest;
import in.projecteka.consentmanager.consent.model.HIPReference;
import in.projecteka.consentmanager.consent.model.HIUReference;
import in.projecteka.consentmanager.consent.model.PatientReference;
import in.projecteka.consentmanager.consent.model.request.RequestedDetail;
import in.projecteka.consentmanager.consent.repository.ConsentArtefactRepository;
import in.projecteka.consentmanager.consent.repository.ConsentRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.security.KeyPair;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class ConsentManagerTest {

    @Mock
    private ConsentRequestRepository repository;
    @Mock
    private ConsentArtefactRepository consentArtefactRepository;
    @Mock
    private ClientRegistryClient providerClient;
    @Mock
    private UserServiceClient userClient;
    @Mock
    private PostConsentApproval postConsentApproval;
    @Mock
    private PostConsentRequestNotification postConsentRequestNotification;

    @MockBean
    private ConsentRequestNotificationListener consentRequestNotificationListener;

    @MockBean
    private KeyPair keyPair;
    private ConsentManager consentManager;

    @Captor
    private ArgumentCaptor<ConsentRequest> captor;

    @BeforeEach
    public void setUp() {
        initMocks(this);
        consentManager = new ConsentManager(providerClient,
                userClient,
                repository,
                consentArtefactRepository,
                keyPair,
                postConsentApproval,
                postConsentRequestNotification);
    }

    @Test
    public void askForConsent() {
        HIPReference hip1 = HIPReference.builder().id("hip1").build();
        HIUReference hiu1 = HIUReference.builder().id("hiu1").build();
        PatientReference patient = PatientReference.builder().id("chethan@ncg").build();
        RequestedDetail requestedDetail = RequestedDetail.builder().hip(hip1).hiu(hiu1).patient(patient).build();

        when(postConsentRequestNotification.broadcastConsentRequestNotification(captor.capture()))
                .thenReturn(Mono.empty());
        when(repository.insert(any(), any())).thenReturn(Mono.empty());
        when(providerClient.providerWith(eq("hip1"))).thenReturn(Mono.just(new Provider()));
        when(providerClient.providerWith(eq("hiu1"))).thenReturn(Mono.just(new Provider()));
        when(userClient.userOf(eq("chethan@ncg"))).thenReturn(Mono.just(new User()));

        String requestingHIUId = "hiu1";
        StepVerifier.create(consentManager.askForConsent(requestingHIUId, requestedDetail)).expectNextMatches(r -> r != null).verifyComplete();
    }

    @Test
    public void askForConsentWithoutValidHIU() {
        HIPReference hip1 = HIPReference.builder().id("hip1").build();
        HIUReference hiu1 = HIUReference.builder().id("hiu1").build();
        PatientReference patient = PatientReference.builder().id("chethan@ncg").build();
        RequestedDetail requestedDetail = RequestedDetail.builder().hip(hip1).hiu(hiu1).patient(patient).build();

        when(postConsentRequestNotification.broadcastConsentRequestNotification(captor.capture()))
                .thenReturn(Mono.empty());
        when(repository.insert(any(), any())).thenReturn(Mono.empty());
        when(providerClient.providerWith(eq("hip1"))).thenReturn(Mono.just(new Provider()));
        when(providerClient.providerWith(eq("hiu1"))).thenReturn(Mono.error(ClientError.providerNotFound()));
        when(userClient.userOf(eq("chethan@ncg"))).thenReturn(Mono.just(new User()));

        String requestingHIUId = "hiu1";
        StepVerifier.create(consentManager.askForConsent(requestingHIUId, requestedDetail))
                .expectErrorMatches(e -> (e instanceof ClientError) && ((ClientError) e).getHttpStatus().is4xxClientError());
    }
}