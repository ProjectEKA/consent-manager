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
import in.projecteka.consentmanager.consent.model.HIPConsentArtefactRepresentation;
import in.projecteka.consentmanager.consent.model.HIPReference;
import in.projecteka.consentmanager.consent.model.HIUReference;
import in.projecteka.consentmanager.consent.model.ListResult;
import in.projecteka.consentmanager.consent.model.PatientReference;
import in.projecteka.consentmanager.consent.model.RevokeRequest;
import in.projecteka.consentmanager.consent.model.request.RequestedDetail;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactLightRepresentation;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactRepresentation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.security.KeyPair;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static in.projecteka.consentmanager.consent.TestBuilders.artefactLightRepresentation;
import static in.projecteka.consentmanager.consent.TestBuilders.consentArtefactRepresentation;
import static in.projecteka.consentmanager.consent.TestBuilders.consentRepresentation;
import static in.projecteka.consentmanager.consent.TestBuilders.consentRequestDetail;
import static in.projecteka.consentmanager.consent.TestBuilders.hipConsentArtefactRepresentation;
import static in.projecteka.consentmanager.consent.TestBuilders.string;
import static in.projecteka.consentmanager.consent.model.ConsentStatus.DENIED;
import static in.projecteka.consentmanager.consent.model.ConsentStatus.EXPIRED;
import static in.projecteka.consentmanager.consent.model.ConsentStatus.GRANTED;
import static in.projecteka.consentmanager.consent.model.ConsentStatus.REQUESTED;
import static in.projecteka.consentmanager.consent.model.ConsentStatus.REVOKED;
import static in.projecteka.consentmanager.dataflow.Utils.toDateWithMilliSeconds;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

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
        CMProperties cmProperties = new CMProperties("NCG");
        ConsentArtefactQueryGenerator queryGenerator = new ConsentArtefactQueryGenerator();
        consentManager = new ConsentManager(userClient,
                repository,
                consentArtefactRepository,
                keyPair,
                consentNotificationPublisher,
                centralRegistry,
                postConsentRequestNotification,
                patientServiceClient,
                cmProperties,
                queryGenerator);
    }

    @Test
    public void getConsents() throws ParseException {
        ConsentRepresentation consentRepresentation = consentRepresentation().build();
        consentRepresentation.setStatus(ConsentStatus.GRANTED);
        String consentRequestId = consentRepresentation.getConsentRequestId();
        String consentId = consentRepresentation.getConsentDetail().getConsentId();
        String patientId = "user@ncg";
        consentRepresentation.getConsentDetail().getPatient().setId(patientId);
        consentRepresentation.getConsentDetail().getHiu().setId(patientId);
        String[] consentIds = new String[]{consentId};
        ConsentArtefactRepresentation consentArtefactRepresentation = consentArtefactRepresentation().build();
        consentArtefactRepresentation.setConsentDetail(consentRepresentation.getConsentDetail());
        consentArtefactRepresentation.getConsentDetail().getPermission().setDataEraseAt(toDateWithMilliSeconds("253379772420000"));

        when(centralRegistry.providerWith(eq("hiu1"))).thenReturn(Mono.just(new Provider()));
        when(consentArtefactRepository.getConsentArtefacts(consentRequestId)).thenReturn(Flux.fromArray(consentIds));
        when(consentArtefactRepository.getConsentArtefact(consentId)).thenReturn(Mono.just(consentArtefactRepresentation));

        StepVerifier.create(consentManager.getConsents(consentRequestId, patientId)
                .subscriberContext(context -> context.put(HttpHeaders.AUTHORIZATION, string())))
                .expectNext(consentArtefactRepresentation)
                .expectComplete()
                .verify();
        verify(consentArtefactRepository, times(0)).updateConsentArtefactStatus(any(), any());
        verifyNoInteractions(consentNotificationPublisher);
    }

    @Test
    public void getConsent() throws ParseException {
        ConsentRepresentation consentRepresentation = consentRepresentation().build();
        consentRepresentation.setStatus(ConsentStatus.GRANTED);
        String consentRequestId = consentRepresentation.getConsentRequestId();
        String patientId = consentRepresentation.getConsentDetail().getPatient().getId();
        consentRepresentation.getConsentDetail().getHiu().setId(patientId);
        ConsentArtefactRepresentation consentArtefactRepresentation = consentArtefactRepresentation().build();
        consentArtefactRepresentation.setConsentDetail(consentRepresentation.getConsentDetail());
        consentArtefactRepresentation.getConsentDetail().getPermission().setDataEraseAt(toDateWithMilliSeconds("253379772420000"));

        when(centralRegistry.providerWith(eq("hiu1"))).thenReturn(Mono.just(new Provider()));
        when(consentArtefactRepository.getConsentArtefact(consentRequestId)).thenReturn(Mono.just(consentArtefactRepresentation));

        StepVerifier.create(consentManager.getConsent(consentRequestId, patientId)
                .subscriberContext(context -> context.put(HttpHeaders.AUTHORIZATION, string())))
                .expectNext(consentArtefactRepresentation)
                .expectComplete()
                .verify();
        verify(consentArtefactRepository, times(0)).updateConsentArtefactStatus(any(), any());
        verifyNoInteractions(consentNotificationPublisher);
    }

    @Test
    public void getConsentArtefactLight() throws ParseException {
        ConsentRepresentation consentRepresentation = consentRepresentation().build();
        consentRepresentation.setStatus(ConsentStatus.GRANTED);
        String patientId = consentRepresentation.getConsentDetail().getPatient().getId();
        String consentId = consentRepresentation.getConsentDetail().getConsentId();
        consentRepresentation.getConsentDetail().getHiu().setId(patientId);
        ConsentArtefactRepresentation consentArtefactRepresentation = consentArtefactRepresentation().build();
        consentArtefactRepresentation.setConsentDetail(consentRepresentation.getConsentDetail());
        consentArtefactRepresentation.setStatus(ConsentStatus.GRANTED);
        consentArtefactRepresentation.getConsentDetail().getPermission().setDataEraseAt(toDateWithMilliSeconds("253379772420000"));
        HIPConsentArtefactRepresentation hipConsentArtefactRepresentation = hipConsentArtefactRepresentation().build();
        ConsentArtefactLightRepresentation artefactLightRepresentation = artefactLightRepresentation().build();
        artefactLightRepresentation.getConsentDetail().setHiu(consentArtefactRepresentation.getConsentDetail().getHiu());
        artefactLightRepresentation.getConsentDetail().setPermission(consentArtefactRepresentation.getConsentDetail().getPermission());
        artefactLightRepresentation.setSignature(hipConsentArtefactRepresentation.getSignature());
        artefactLightRepresentation.setStatus(consentArtefactRepresentation.getStatus());

        when(centralRegistry.providerWith(eq("hiu1"))).thenReturn(Mono.just(new Provider()));
        when(consentArtefactRepository.getHipConsentArtefact(consentId)).thenReturn(Mono.just(hipConsentArtefactRepresentation));
        when(consentArtefactRepository.getConsentArtefact(consentId)).thenReturn(Mono.just(consentArtefactRepresentation));

        StepVerifier.create(consentManager.getConsentArtefactLight(consentId)
                .subscriberContext(context -> context.put(HttpHeaders.AUTHORIZATION, string())))
                .expectNext(artefactLightRepresentation)
                .expectComplete()
                .verify();
        verify(consentArtefactRepository, times(0)).updateConsentArtefactStatus(any(), any());
        verifyNoInteractions(consentNotificationPublisher);
    }

    @Test
    public void askForConsent() {
        var requestId = UUID.randomUUID();
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
        when(repository.requestOf(requestId.toString())).thenReturn(Mono.empty());

        StepVerifier.create(consentManager.askForConsent(requestedDetail, requestId))
                .expectNextMatches(Objects::nonNull)
                .verifyComplete();
    }

    @Test
    public void askForConsentWithoutValidHIU() {
        var requestId = UUID.randomUUID();
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
        when(repository.requestOf(requestId.toString())).thenReturn(Mono.just(consentRequestDetail().build()));

        StepVerifier.create(consentManager.askForConsent(requestedDetail, requestId))
                .expectErrorMatches(e -> (e instanceof ClientError) &&
                        ((ClientError) e).getHttpStatus().is4xxClientError())
                .verify();
    }

    @Test
    public void revokeAndBroadCastConsent() {
        ConsentRepresentation consentRepresentation = consentRepresentation().build();
        consentRepresentation.setStatus(GRANTED);
        ConsentRequestDetail consentRequestDetail = consentRequestDetail().build();
        consentRequestDetail.setStatus(GRANTED);
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
        when(repository.requestOf(consentRequestId, GRANTED.toString(), patientId))
                .thenReturn(Mono.just(consentRequestDetail));
        when(consentArtefactRepository.updateStatus(consentId, consentRequestId, REVOKED)).thenReturn(Mono.empty());
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
                .status(REQUESTED);
        when(repository.requestOf(requestId))
                .thenReturn(Mono.just(consentRequestDetail.build()),
                        Mono.just(consentRequestDetail.status(DENIED).build()));
        when(repository.updateStatus(requestId, DENIED)).thenReturn(Mono.empty());
        when(consentNotificationPublisher.publish(any())).thenReturn(Mono.empty());

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
                .status(REQUESTED)
                .build();
        when(repository.requestOf(requestId)).thenReturn(Mono.just(consentRequestDetail));

        Mono<? extends Void> publisher = consentManager.deny(requestId, patientId);

        StepVerifier.create(publisher)
                .expectErrorMatches(e -> (e instanceof ClientError) &&
                        ((ClientError) e).getHttpStatus() == FORBIDDEN)
                .verify();
    }

    @Test
    void returnConflictDenyConsentRequest() {
        var patientId = string();
        var requestId = string();
        var consentRequestDetail = consentRequestDetail()
                .requestId(requestId)
                .patient(new PatientReference(patientId))
                .status(GRANTED)
                .build();
        when(repository.requestOf(requestId)).thenReturn(Mono.just(consentRequestDetail));

        Mono<? extends Void> publisher = consentManager.deny(requestId, patientId);

        StepVerifier.create(publisher)
                .expectErrorMatches(e -> (e instanceof ClientError) &&
                        ((ClientError) e).getHttpStatus() == CONFLICT)
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
                        ((ClientError) e).getHttpStatus() == NOT_FOUND)
                .verify();
    }

    @Test
    void shouldGetAllTheConsentArtefacts() {
        ConsentArtefactRepresentation artefact1 = consentArtefactRepresentation().status(GRANTED).build();
        ConsentArtefactRepresentation artefact2 = consentArtefactRepresentation().status(REVOKED).build();
        ConsentArtefactRepresentation artefact3 = consentArtefactRepresentation().status(EXPIRED).build();
        List<ConsentArtefactRepresentation> response = new ArrayList<>();
        response.add(artefact1);
        response.add(artefact2);
        response.add(artefact3);

        ListResult<List<ConsentArtefactRepresentation>> result = new ListResult<>(response, response.size());

        when(consentArtefactRepository.getAllConsentArtefacts("shweta@ncg", 20, 0, null))
                .thenReturn(Mono.just(result));
        Mono<ListResult<List<ConsentArtefactRepresentation>>> publisher =
                consentManager.getAllConsentArtefacts("shweta@ncg", 20, 0, "ALL");
        StepVerifier.create(publisher
                .subscriberContext(context -> context.put(HttpHeaders.AUTHORIZATION, string())))
                .expectNext(result)
                .expectComplete()
                .verify();
    }

    @Test
    void shouldGetEmptyIfNoConsentArtefactsPresent() {
        when(consentArtefactRepository.getAllConsentArtefacts("shweta@ncg", 20, 0, null))
                .thenReturn(Mono.empty());
        Mono<ListResult<List<ConsentArtefactRepresentation>>> publisher =
                consentManager.getAllConsentArtefacts("shweta@ncg", 20, 0, "ALL");
        StepVerifier.create(publisher
                .subscriberContext(context -> context.put(HttpHeaders.AUTHORIZATION, string())))
                .expectComplete()
                .verify();
    }
}