package in.projecteka.consentmanager.consent.service;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.ClientRegistryClient;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.clients.model.Provider;
import in.projecteka.consentmanager.clients.model.User;
import in.projecteka.consentmanager.consent.ConsentManager;
import in.projecteka.consentmanager.consent.model.request.ConsentDetail;
import in.projecteka.consentmanager.consent.model.request.HIPReference;
import in.projecteka.consentmanager.consent.model.request.HIUReference;
import in.projecteka.consentmanager.consent.model.request.PatientReference;
import in.projecteka.consentmanager.consent.repository.ConsentArtefactRepository;
import in.projecteka.consentmanager.consent.repository.ConsentRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    @MockBean
    private KeyPair keyPair;

    @BeforeEach
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void askForConsent() {
        HIPReference hip1 = HIPReference.builder().id("hip1").build();
        HIUReference hiu1 = HIUReference.builder().id("hiu1").build();
        PatientReference patient = PatientReference.builder().id("chethan@ncg").build();
        ConsentDetail consentDetail = ConsentDetail.builder().hip(hip1).hiu(hiu1).patient(patient).build();

        ConsentManager consentManager = new ConsentManager(repository, providerClient, userClient, consentArtefactRepository, keyPair);
        when(repository.insert(any(), any())).thenReturn(Mono.empty());
        when (providerClient.providerWith(eq("hip1"))).thenReturn(Mono.just(new Provider()));
        when (providerClient.providerWith(eq("hiu1"))).thenReturn(Mono.just(new Provider()));
        when (userClient.userOf(eq("chethan@ncg"))).thenReturn(Mono.just(new User()));

        String requestingHIUId = "hiu1";
        StepVerifier.create(consentManager.askForConsent(requestingHIUId, consentDetail)).expectNextMatches(r -> r != null).verifyComplete();
    }

    @Test
    public void askForConsentWithoutValidHIU() {
        HIPReference hip1 = HIPReference.builder().id("hip1").build();
        HIUReference hiu1 = HIUReference.builder().id("hiu1").build();
        PatientReference patient = PatientReference.builder().id("chethan@ncg").build();
        ConsentDetail consentDetail = ConsentDetail.builder().hip(hip1).hiu(hiu1).patient(patient).build();

        ConsentManager consentManager = new ConsentManager(repository, providerClient, userClient, consentArtefactRepository, keyPair);
        when(repository.insert(any(), any())).thenReturn(Mono.empty());
        when (providerClient.providerWith(eq("hip1"))).thenReturn(Mono.just(new Provider()));
        when (providerClient.providerWith(eq("hiu1"))).thenReturn(Mono.error(ClientError.providerNotFound()));
        when (userClient.userOf(eq("chethan@ncg"))).thenReturn(Mono.just(new User()));

        String requestingHIUId = "hiu1";
        StepVerifier.create(consentManager.askForConsent(requestingHIUId, consentDetail))
                .expectErrorMatches(e -> (e instanceof ClientError) && ((ClientError) e).getHttpStatus().is4xxClientError());
    }
}