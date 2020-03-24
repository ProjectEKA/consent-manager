package in.projecteka.consentmanager.consent;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.PatientServiceClient;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.clients.model.Provider;
import in.projecteka.consentmanager.clients.model.User;
import in.projecteka.consentmanager.common.CentralRegistry;
import in.projecteka.consentmanager.consent.model.*;
import in.projecteka.consentmanager.consent.model.request.RequestedDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Objects;

import static in.projecteka.consentmanager.consent.TestBuilders.string;
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
    private CentralRegistry centralRegistry;
    @Mock
    private UserServiceClient userClient;
    @Mock
    private ConsentNotificationPublisher consentNotificationPublisher;
    @Mock
    private PostConsentRequest postConsentRequestNotification;
    @Mock
    private PatientServiceClient patientServiceClient;
    @Mock
    private ConsentRepresentation consentRepresentation;
    @Mock
    private ConsentRequestRepository consentRequestRepository;


    @SuppressWarnings("unused")
    @MockBean
    private ConsentRequestNotificationListener consentRequestNotificationListener;

    @MockBean
    private KeyPair keyPair;
    private ConsentManager consentManager;

    @Captor
    private ArgumentCaptor<ConsentRequest> captor;

    @BeforeEach
    public void setUp() throws JOSEException {
        initMocks(this);
        RSAKeyGenerator rsKG = new RSAKeyGenerator(2048);
        keyPair = rsKG.generate().toKeyPair();
        consentManager = new ConsentManager(userClient,
                repository,
                consentArtefactRepository,
                keyPair,
                consentNotificationPublisher,
                centralRegistry,
                postConsentRequestNotification,
                patientServiceClient);
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
        when(centralRegistry.providerWith(eq("hip1"))).thenReturn(Mono.just(new Provider()));
        when(centralRegistry.providerWith(eq("hiu1"))).thenReturn(Mono.just(new Provider()));
        when(userClient.userOf(eq("chethan@ncg"))).thenReturn(Mono.just(new User()));

        StepVerifier.create(
                consentManager.askForConsent(requestedDetail)
                        .subscriberContext(context -> context.put(HttpHeaders.AUTHORIZATION, string())))
                .expectNextMatches(Objects::nonNull)
                .verifyComplete();
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
        when(centralRegistry.providerWith(eq("hip1"))).thenReturn(Mono.just(new Provider()));
        when(centralRegistry.providerWith(eq("hiu1"))).thenReturn(Mono.error(ClientError.providerNotFound()));
        when(userClient.userOf(eq("chethan@ncg"))).thenReturn(Mono.just(new User()));

        StepVerifier.create(consentManager.askForConsent(requestedDetail)
                .subscriberContext(context -> context.put(HttpHeaders.AUTHORIZATION, string())))
                .expectErrorMatches(e -> (e instanceof ClientError) &&
                        ((ClientError) e).getHttpStatus().is4xxClientError())
                .verify();
    }

    @Test
    public void revokeAndBroadCastConsent() {
        PatientReference patient = PatientReference.builder().id("chethan@ncg").build();
        HIUReference hiuRef = HIUReference.builder()
                .id("chethan@ncg")
                .build();
        ConsentArtefact cArtefact = ConsentArtefact.builder()
                .hiu(hiuRef)
                .patient(patient)
                .consentId("10000005")
                .build();
        ConsentRepresentation consentRepresentation = ConsentRepresentation.builder()
                .consentRequestId("111")
                .consentDetail(cArtefact)
                .status(ConsentStatus.GRANTED)
                .build();

        ConsentRequestDetail consentRequestDetail = ConsentRequestDetail.builder()
                .requestId("111")
                .status(ConsentStatus.GRANTED)
                .patient(patient)
                .hiu(hiuRef)
                .build();
        ArrayList<String> consents = new ArrayList<String>();
        consents.add("10000005");
        RevokeRequest revokeRequest = RevokeRequest.builder().consents(consents).build();

        when(centralRegistry.providerWith(eq("hip1"))).thenReturn(Mono.just(new Provider()));
        when(centralRegistry.providerWith(eq("hiu1"))).thenReturn(Mono.just(new Provider()));
        when(consentArtefactRepository.getConsentWithRequest("10000005")).thenReturn(Mono.just(consentRepresentation));
        when(repository.requestOf(eq("111"), eq(ConsentStatus.GRANTED.toString()), eq("chethan@ncg"))).thenReturn(Mono.just(consentRequestDetail));
        when(consentArtefactRepository.updateStatus("10000005", "111", ConsentStatus.REVOKED)).thenReturn(Mono.empty());
        when(consentNotificationPublisher.broadcastConsentArtefacts(any())).thenReturn(Mono.empty());

        StepVerifier.create(consentManager.revokeAndBroadCastConsent(revokeRequest, "chethan@ncg")
                .subscriberContext(context -> context.put(HttpHeaders.AUTHORIZATION, string())))
                .verifyComplete();
    }

    @Test
    public void revokeAndBroadCastConsentWithIncorrectConsentStatus() {
        PatientReference patient = PatientReference.builder().id("chethan@ncg").build();
        HIUReference hiuRef = HIUReference.builder()
                .id("chethan@ncg")
                .build();
        ConsentArtefact cArtefact = ConsentArtefact.builder()
                .hiu(hiuRef)
                .patient(patient)
                .consentId("10000005")
                .build();
        ConsentRepresentation consentRepresentation = ConsentRepresentation.builder()
                .consentRequestId("111")
                .consentDetail(cArtefact)
                .status(ConsentStatus.REQUESTED)
                .build();

        ArrayList<String> consents = new ArrayList<String>();
        consents.add("10000005");
        RevokeRequest revokeRequest = RevokeRequest.builder().consents(consents).build();

        when(centralRegistry.providerWith(eq("hip1"))).thenReturn(Mono.just(new Provider()));
        when(centralRegistry.providerWith(eq("hiu1"))).thenReturn(Mono.just(new Provider()));
        when(consentArtefactRepository.getConsentWithRequest("10000005")).thenReturn(Mono.just(consentRepresentation));
        when(repository.requestOf(eq("111"), eq(ConsentStatus.GRANTED.toString()), eq("chethan@ncg"))).thenReturn(Mono.error(ClientError.consentArtefactForbidden()));

        StepVerifier.create(consentManager.revokeAndBroadCastConsent(revokeRequest, "chethan@ncg")
                .subscriberContext(context -> context.put(HttpHeaders.AUTHORIZATION, string())))
                .expectErrorMatches(e -> (e instanceof ClientError) &&
                        ((ClientError) e).getHttpStatus().is4xxClientError())
                .verify();
    }
}