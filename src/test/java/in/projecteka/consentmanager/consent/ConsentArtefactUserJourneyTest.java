package in.projecteka.consentmanager.consent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.model.Error;
import in.projecteka.consentmanager.clients.model.ErrorCode;
import in.projecteka.consentmanager.clients.model.ErrorRepresentation;
import in.projecteka.consentmanager.common.Authenticator;
import in.projecteka.consentmanager.common.Caller;
import in.projecteka.consentmanager.consent.model.ConsentRepresentation;
import in.projecteka.consentmanager.consent.model.ConsentRequestDetail;
import in.projecteka.consentmanager.consent.model.ConsentStatus;
import in.projecteka.consentmanager.consent.model.RevokeRequest;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactRepresentation;
import in.projecteka.consentmanager.dataflow.DataFlowBroadcastListener;
import okhttp3.mockwebserver.MockWebServer;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static in.projecteka.consentmanager.consent.TestBuilders.consentArtefactRepresentation;
import static in.projecteka.consentmanager.consent.TestBuilders.consentRepresentation;
import static in.projecteka.consentmanager.consent.TestBuilders.consentRequestDetail;
import static in.projecteka.consentmanager.consent.TestBuilders.string;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@AutoConfigureWebTestClient
@ContextConfiguration(initializers =
        in.projecteka.consentmanager.consent.ConsentArtefactUserJourneyTest.ContextInitializer.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ConsentArtefactUserJourneyTest {
    private static MockWebServer clientRegistryServer = new MockWebServer();
    private static MockWebServer userServer = new MockWebServer();

    @Autowired
    private WebTestClient webTestClient;

    @SuppressWarnings("unused")
    @MockBean
    private DestinationsConfig destinationsConfig;

    @SuppressWarnings("unused")
    @MockBean
    private HiuConsentNotificationListener hiuConsentNotificationListener;

    @MockBean
    private PinVerificationTokenService pinVerificationTokenService;

    @MockBean
    private HipConsentNotificationListener hipConsentNotificationListener;

    @SuppressWarnings("unused")
    @MockBean
    private DataFlowBroadcastListener dataFlowBroadcastListener;

    @SuppressWarnings("unused")
    @MockBean
    private ConsentRequestNotificationListener consentRequestNotificationListener;

    @MockBean
    private ConsentArtefactRepository consentArtefactRepository;

    @MockBean
    private ConsentNotificationPublisher consentNotificationPublisher;

    @MockBean
    private ConsentRequestRepository repository;

    @MockBean
    private Authenticator authenticator;

    @SuppressWarnings("unused")
    @MockBean(name = "centralRegistryJWKSet")
    private JWKSet centralRegistryJWKSet;

    @SuppressWarnings("unused")
    @MockBean(name = "identityServiceJWKSet")
    private JWKSet identityServiceJWKSet;

    @AfterAll
    public static void tearDown() throws IOException {
        clientRegistryServer.shutdown();
        userServer.shutdown();
    }

    @Test
    public void shouldListConsentArtifacts() {
        var consentArtefact = consentArtefactRepresentation().build();
        var token = string();
        var patientId = consentArtefact.getConsentDetail().getPatient().getId();
        var consentRequestId = "request-id";
        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller(patientId, false)));
        when(consentArtefactRepository.getConsentArtefacts(eq(consentRequestId)))
                .thenReturn(Flux.just(consentArtefact.getConsentDetail().getConsentId()));
        when(consentArtefactRepository.getConsentArtefact(eq(consentArtefact.getConsentDetail().getConsentId())))
                .thenReturn(Mono.just(consentArtefact));

        webTestClient.get()
                .uri("/consent-requests/" + consentRequestId + "/consent-artefacts")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<ConsentArtefactRepresentation>>() {
                })
                .value(value -> value.get(0).getConsentDetail(), Matchers.equalTo(consentArtefact.getConsentDetail()))
                .value(value -> value.get(0).getStatus(), Matchers.is(consentArtefact.getStatus()))
                .value(value -> value.get(0).getSignature(), Matchers.is(consentArtefact.getSignature()));
    }

    @Test
    public void shouldThrowConsentArtifactNotFound() throws JsonProcessingException {
        var token = string();
        var consentArtefact = consentArtefactRepresentation().build();
        var patientId = consentArtefact.getConsentDetail().getPatient().getId();
        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller(patientId, false)));
        var errorResponse = new ErrorRepresentation(new Error(ErrorCode.CONSENT_ARTEFACT_NOT_FOUND, "Cannot find the " +
                "consent artefact"));
        var errorResponseJson = new ObjectMapper().writeValueAsString(errorResponse);
        var consentRequestId = "request-id";
        when(consentArtefactRepository.getConsentArtefacts(eq(consentRequestId)))
                .thenReturn(Flux.empty());
        when(consentArtefactRepository.getConsentArtefact(eq(consentArtefact.getConsentDetail().getConsentId())))
                .thenReturn(Mono.just(consentArtefact));

        webTestClient.get()
                .uri("/consent-requests/" + consentRequestId + "/consent-artefacts")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", token)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .json(errorResponseJson);
    }

    @Test
    public void shouldThrowInvalidRequester() throws JsonProcessingException {
        var token = string();
        var anotherUser = string();
        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller(anotherUser, false)));
        var consentArtefact = consentArtefactRepresentation().build();
        var errorResponse = new ErrorRepresentation(new Error(ErrorCode.CONSENT_ARTEFACT_FORBIDDEN,
                "Cannot retrieve Consent artefact. Forbidden"));
        var errorResponseJson = new ObjectMapper().writeValueAsString(errorResponse);
        var consentRequestId = "request-id";
        when(consentArtefactRepository.getConsentArtefacts(eq(consentRequestId)))
                .thenReturn(Flux.just(consentArtefact.getConsentDetail().getConsentId()));
        when(consentArtefactRepository.getConsentArtefact(eq(consentArtefact.getConsentDetail().getConsentId())))
                .thenReturn(Mono.just(consentArtefact));

        webTestClient.get()
                .uri("/consent-requests/" + consentRequestId + "/consent-artefacts")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", token)
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody()
                .json(errorResponseJson);
    }

    @Test
    public void shouldRevokeConsentArtefact() {
        var token = string();
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

        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller("user", false)));
        when(pinVerificationTokenService.validateToken(token))
                .thenReturn(Mono.just(new Caller(patientId, false)));
        when(consentArtefactRepository.getConsentWithRequest(eq(consentId)))
                .thenReturn(Mono.just(consentRepresentation));
        when(repository.requestOf(consentRequestId, ConsentStatus.GRANTED.toString(), patientId))
                .thenReturn(Mono.just(consentRequestDetail));
        when(consentArtefactRepository.updateStatus(consentId, consentRequestId, ConsentStatus.REVOKED))
                .thenReturn(Mono.empty());
        when(consentNotificationPublisher.broadcastConsentArtefacts(any())).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/consents/revoke")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", token)
                .bodyValue(revokeRequest)
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    public void shouldNotRevokeConsentArtefactWhenItIsNotInGrantedState() {
        var token = string();
        ConsentRepresentation consentRepresentation = consentRepresentation().build();
        consentRepresentation.setStatus(ConsentStatus.REVOKED);
        ConsentRequestDetail consentRequestDetail = consentRequestDetail().build();
        consentRequestDetail.setStatus(ConsentStatus.REVOKED);
        String consentRequestId = consentRepresentation.getConsentRequestId();
        consentRequestDetail.setRequestId(consentRequestId);
        List<String> consentIds = new ArrayList<>();
        String consentId = consentRepresentation.getConsentDetail().getConsentId();
        consentIds.add(consentRepresentation.getConsentDetail().getConsentId());
        RevokeRequest revokeRequest = RevokeRequest.builder().consents(consentIds).build();
        String patientId = consentRepresentation.getConsentDetail().getPatient().getId();

        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller("user", false)));
        when(pinVerificationTokenService.validateToken(token))
                .thenReturn(Mono.just(new Caller(patientId, false)));
        when(consentArtefactRepository.getConsentWithRequest(eq(consentId)))
                .thenReturn(Mono.just(consentRepresentation));
        when(repository.requestOf(consentRequestId, ConsentStatus.GRANTED.toString(), patientId))
                .thenReturn(Mono.error(new Exception("Consent request with given id, status and patientId not found")));

        webTestClient.post()
                .uri("/consents/revoke")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", token)
                .bodyValue(revokeRequest)
                .exchange()
                .expectStatus()
                .is5xxServerError();

        verify(consentArtefactRepository, times(0)).updateStatus(any(), any(), any());
        verifyNoInteractions(consentNotificationPublisher);
    }

    public static class ContextInitializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertyValues values =
                    TestPropertyValues.of(
                            Stream.of("consentmanager.clientregistry.url=" + clientRegistryServer.url(""),
                                    "consentmanager.userservice.url=" + userServer.url(""),
                                    "consentmanager.consentservice.maxPageSize=50"));
            values.applyTo(applicationContext);
        }
    }
}