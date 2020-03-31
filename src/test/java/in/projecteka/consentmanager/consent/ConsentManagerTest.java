package in.projecteka.consentmanager.consent;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.PatientServiceClient;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.clients.model.Provider;
import in.projecteka.consentmanager.clients.model.User;
import in.projecteka.consentmanager.common.CentralRegistry;
import in.projecteka.consentmanager.consent.model.ConsentRepresentation;
import in.projecteka.consentmanager.consent.model.ConsentRequest;
import in.projecteka.consentmanager.consent.model.ConsentRequestDetail;
import in.projecteka.consentmanager.consent.model.ConsentStatus;
import in.projecteka.consentmanager.consent.model.HIPReference;
import in.projecteka.consentmanager.consent.model.HIUReference;
import in.projecteka.consentmanager.consent.model.PatientReference;
import in.projecteka.consentmanager.consent.model.RevokeRequest;
import in.projecteka.consentmanager.consent.model.request.RequestedDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static in.projecteka.consentmanager.consent.TestBuilders.consentRepresentation;
import static in.projecteka.consentmanager.consent.TestBuilders.consentRequestDetail;
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

        StepVerifier.create(consentManager.askForConsent(requestedDetail))
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

        StepVerifier.create(consentManager.askForConsent(requestedDetail))
                .expectErrorMatches(e -> (e instanceof ClientError) &&
                        ((ClientError) e).getHttpStatus().is4xxClientError())
                .verify();
    }

    @Test
    public void revokeAndBroadCastConsent() {
        ConsentRepresentation consentRepresentation = consentRepresentation().build();
        consentRepresentation.setStatus(ConsentStatus.GRANTED);
        ConsentRequestDetail consentRequestDetail = consentRequestDetail().build();
        consentRequestDetail.setStatus(ConsentStatus.GRANTED);
        String consentRequestId = consentRepresentation.getConsentRequestId();
        consentRequestDetail.setRequestId(consentRequestId);
        List<String> consentIds = new ArrayList<>();
        String consentId = consentRepresentation.getConsentDetail().getConsentId();
        consentIds.add(consentRepresentation.getConsentDetail().getConsentId());
        RevokeRequest revokeRequest = RevokeRequest.builder().consents(consentIds).build();
        String patientId = consentRepresentation.getConsentDetail().getPatient().getId();

        when(centralRegistry.providerWith(eq("hip1"))).thenReturn(Mono.just(new Provider()));
        when(centralRegistry.providerWith(eq("hiu1"))).thenReturn(Mono.just(new Provider()));
        when(consentArtefactRepository.getConsentWithRequest(consentId)).thenReturn(Mono.just(consentRepresentation));
        when(repository.requestOf(consentRequestId, ConsentStatus.GRANTED.toString(), patientId)).thenReturn(Mono.just(consentRequestDetail));
        when(consentArtefactRepository.updateStatus(consentId, consentRequestId, ConsentStatus.REVOKED)).thenReturn(Mono.empty());
        when(consentNotificationPublisher.publish(any())).thenReturn(Mono.empty());

        StepVerifier.create(consentManager.revoke(revokeRequest, patientId))
                .verifyComplete();
    }

    @Test
    void denyConsentRequest() {
        var patientId = string();
        var requestId = string();
        var consentRequestDetail = consentRequestDetail()
                .requestId(requestId)
                .patient(new PatientReference(patientId))
                .status(ConsentStatus.REQUESTED)
                .build();
        when(repository.requestOf(requestId)).thenReturn(Mono.just(consentRequestDetail));
        when(repository.updateStatus(requestId, ConsentStatus.DENIED)).thenReturn(Mono.empty());

        Mono<? extends Void> publisher = consentManager.deny(requestId, patientId);

        StepVerifier.create(publisher)
                .verifyComplete();
    }

    @Test
    void returnForbiddenDenyConsentRequest() {
        var patientId = string();
        var differentPatientId = string();
        var requestId = string();
        var consentRequestDetail = consentRequestDetail()
                .requestId(requestId)
                .patient(new PatientReference(differentPatientId))
                .status(ConsentStatus.REQUESTED)
                .build();
        when(repository.requestOf(requestId)).thenReturn(Mono.just(consentRequestDetail));

        Mono<? extends Void> publisher = consentManager.deny(requestId, patientId);

        StepVerifier.create(publisher)
                .expectErrorMatches(e -> (e instanceof ClientError) &&
                        ((ClientError) e).getHttpStatus() == HttpStatus.FORBIDDEN)
                .verify();
    }

    @Test
    void returnConflictDenyConsentRequest() {
        var patientId = string();
        var requestId = string();
        var consentRequestDetail = consentRequestDetail()
                .requestId(requestId)
                .patient(new PatientReference(patientId))
                .status(ConsentStatus.GRANTED)
                .build();
        when(repository.requestOf(requestId)).thenReturn(Mono.just(consentRequestDetail));

        Mono<? extends Void> publisher = consentManager.deny(requestId, patientId);

        StepVerifier.create(publisher)
                .expectErrorMatches(e -> (e instanceof ClientError) &&
                        ((ClientError) e).getHttpStatus() == HttpStatus.CONFLICT)
                .verify();
    }

    @Test
    void returnNotFoundDenyConsentRequest() {
        var patientId = string();
        var requestId = string();
        when(repository.requestOf(requestId)).thenReturn(Mono.empty());

        Mono<? extends Void> publisher = consentManager.deny(requestId, patientId);

        StepVerifier.create(publisher)
                .expectErrorMatches(e -> (e instanceof ClientError) &&
                        ((ClientError) e).getHttpStatus() == HttpStatus.NOT_FOUND)
                .verify();
    }
}