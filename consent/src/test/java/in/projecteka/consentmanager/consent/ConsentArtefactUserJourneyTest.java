package in.projecteka.consentmanager.consent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.clients.ConsentManagerClient;
import in.projecteka.consentmanager.consent.model.ConsentStatusCallerDetail;
import in.projecteka.consentmanager.consent.model.ListResult;
import in.projecteka.consentmanager.consent.model.RevokeRequest;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactRepresentation;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactResponse;
import in.projecteka.consentmanager.consent.model.response.ConsentStatusResponse;
import in.projecteka.library.clients.model.Error;
import in.projecteka.library.clients.model.ErrorCode;
import in.projecteka.library.clients.model.ErrorRepresentation;
import in.projecteka.library.clients.model.Provider;
import in.projecteka.library.common.Authenticator;
import in.projecteka.library.common.Caller;
import in.projecteka.library.common.CentralRegistry;
import in.projecteka.library.common.GatewayTokenVerifier;
import in.projecteka.library.common.RequestValidator;
import in.projecteka.library.common.ServiceAuthentication;
import in.projecteka.library.common.ServiceCaller;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static in.projecteka.consentmanager.clients.TestBuilders.toDateWithMilliSeconds;
import static in.projecteka.consentmanager.common.TestBuilders.OBJECT_MAPPER;
import static in.projecteka.consentmanager.consent.TestBuilders.consentArtefactReference;
import static in.projecteka.consentmanager.consent.TestBuilders.consentArtefactRepresentation;
import static in.projecteka.consentmanager.consent.TestBuilders.consentRepresentation;
import static in.projecteka.consentmanager.consent.TestBuilders.consentRequestDetail;
import static in.projecteka.consentmanager.consent.TestBuilders.consentRequestStatus;
import static in.projecteka.consentmanager.consent.TestBuilders.fetchRequest;
import static in.projecteka.consentmanager.consent.TestBuilders.hiuConsentNotificationAcknowledgement;
import static in.projecteka.consentmanager.consent.TestBuilders.string;
import static in.projecteka.consentmanager.consent.model.ConsentStatus.DENIED;
import static in.projecteka.consentmanager.consent.model.ConsentStatus.EXPIRED;
import static in.projecteka.consentmanager.consent.model.ConsentStatus.GRANTED;
import static in.projecteka.consentmanager.consent.model.ConsentStatus.REVOKED;
import static in.projecteka.library.clients.model.ErrorCode.CONSENT_ARTEFACT_FORBIDDEN;
import static in.projecteka.library.clients.model.ErrorCode.CONSENT_NOT_GRANTED;
import static in.projecteka.library.common.Role.GATEWAY;
import static java.lang.Boolean.TRUE;
import static java.lang.String.valueOf;
import static java.util.List.of;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.just;

@ExtendWith(SpringExtension.class)
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = ConsentArtefactUserJourneyTest.PropertyInitializer.class)
class ConsentArtefactUserJourneyTest {

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
    private ConsentRequestNotificationListener consentRequestNotificationListener;

    @MockBean
    private CentralRegistry centralRegistry;

    @MockBean
    private ConsentArtefactRepository consentArtefactRepository;

    @MockBean
    private ConsentNotificationPublisher consentNotificationPublisher;

    @MockBean
    private ConsentRequestRepository repository;

    @MockBean
    private Authenticator authenticator;

    @MockBean
    private ConsentManagerClient consentManagerClient;

    @SuppressWarnings("unused")
    @MockBean(name = "gatewayJWKSet")
    private JWKSet centralRegistryJWKSet;

    @SuppressWarnings("unused")
    @MockBean(name = "identityServiceJWKSet")
    private JWKSet identityServiceJWKSet;

    @SuppressWarnings("unused")
    @MockBean
    private ConceptValidator conceptValidator;

    @MockBean
    private RequestValidator validator;

    @MockBean
    private GatewayTokenVerifier gatewayTokenVerifier;

    @MockBean
    private ServiceAuthentication serviceAuthentication;

    private static final MockWebServer identityServer = new MockWebServer();

    @AfterAll
    static void tearDown() throws IOException {
        identityServer.shutdown();
    }

    @Test
    void shouldListConsentArtifacts() throws ParseException {
        var consentArtefact = consentArtefactRepresentation().build();
        var token = string();
        var patientId = consentArtefact.getConsentDetail().getPatient().getId();
        var consentRequestId = "request-id";
        consentArtefact.getConsentDetail().getPermission().setDataEraseAt(toDateWithMilliSeconds("253379772420000"));
        when(authenticator.verify(token)).thenReturn(just(new Caller(patientId, false)));
        when(consentArtefactRepository.getConsentArtefacts(eq(consentRequestId)))
                .thenReturn(Flux.just(consentArtefact.getConsentDetail().getConsentId()));
        when(consentArtefactRepository.getConsentArtefact(eq(consentArtefact.getConsentDetail().getConsentId())))
                .thenReturn(just(consentArtefact));

        webTestClient.get()
                .uri("/consent-requests/" + consentRequestId + "/consent-artefacts")
                .accept(APPLICATION_JSON)
                .header("Authorization", token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<ConsentArtefactRepresentation>>() {
                })
                .value(value -> value.get(0).getConsentDetail(), equalTo(consentArtefact.getConsentDetail()))
                .value(value -> value.get(0).getStatus(), is(consentArtefact.getStatus()))
                .value(value -> value.get(0).getSignature(), is(consentArtefact.getSignature()));
    }

    @Test
    void shouldThrowConsentArtifactNotFound() throws JsonProcessingException {
        var token = string();
        var consentArtefact = consentArtefactRepresentation().build();
        var patientId = consentArtefact.getConsentDetail().getPatient().getId();
        var errorResponse = new ErrorRepresentation(new Error(ErrorCode.CONSENT_ARTEFACT_NOT_FOUND,
                "Cannot find the consent artefact"));
        var errorResponseJson = OBJECT_MAPPER.writeValueAsString(errorResponse);
        var consentRequestId = "request-id";
        when(authenticator.verify(token)).thenReturn(just(new Caller(patientId, false)));
        when(consentArtefactRepository.getConsentArtefacts(eq(consentRequestId)))
                .thenReturn(Flux.empty());
        when(consentArtefactRepository.getConsentArtefact(eq(consentArtefact.getConsentDetail().getConsentId())))
                .thenReturn(just(consentArtefact));

        webTestClient.get()
                .uri("/consent-requests/" + consentRequestId + "/consent-artefacts")
                .accept(APPLICATION_JSON)
                .header("Authorization", token)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .json(errorResponseJson);
    }

    @Test
    void shouldThrowInvalidRequester() throws JsonProcessingException {
        var token = string();
        var anotherUser = string();
        var consentArtefact = consentArtefactRepresentation().build();
        var errorResponse = new ErrorRepresentation(new Error(CONSENT_ARTEFACT_FORBIDDEN,
                "Cannot find the consent artefact"));
        var errorResponseJson = OBJECT_MAPPER.writeValueAsString(errorResponse);
        var consentRequestId = "request-id";
        when(authenticator.verify(token)).thenReturn(just(new Caller(anotherUser, false)));
        when(consentArtefactRepository.getConsentArtefacts(eq(consentRequestId)))
                .thenReturn(Flux.just(consentArtefact.getConsentDetail().getConsentId()));
        when(consentArtefactRepository.getConsentArtefact(eq(consentArtefact.getConsentDetail().getConsentId())))
                .thenReturn(just(consentArtefact));

        webTestClient.get()
                .uri("/consent-requests/" + consentRequestId + "/consent-artefacts")
                .accept(APPLICATION_JSON)
                .header("Authorization", token)
                .exchange()
                .expectStatus()
                .isEqualTo(403)
                .expectBody()
                .json(errorResponseJson);
    }

    @Test
    void shouldRevokeConsentArtefact() {
        var token = string();
        var consentRepresentation = consentRepresentation().status(GRANTED).build();
        var consentRequestId = consentRepresentation.getConsentRequestId();
        var consentRequestDetail =
                consentRequestDetail().requestId(consentRequestId).status(GRANTED).build();
        List<String> consentIds = new ArrayList<>();
        var consentId = consentRepresentation.getConsentDetail().getConsentId();
        consentIds.add(consentRepresentation.getConsentDetail().getConsentId());
        var revokeRequest = RevokeRequest.builder().consents(consentIds).build();
        var patientId = consentRepresentation.getConsentDetail().getPatient().getId();
        String scope = "consent.revoke";
        when(pinVerificationTokenService.validateToken(token, scope))
                .thenReturn(just(new Caller(patientId, false, "testSessionId")));
        when(consentArtefactRepository.getConsentWithRequest(eq(consentId)))
                .thenReturn(just(consentRepresentation));
        when(repository.requestOf(consentRequestId, GRANTED.toString(), patientId))
                .thenReturn(just(consentRequestDetail));
        when(consentArtefactRepository.updateStatus(consentId, consentRequestId, REVOKED))
                .thenReturn(empty());
        when(consentNotificationPublisher.publish(any())).thenReturn(empty());
        when(centralRegistry.providerWith(any())).thenReturn(just(Provider.builder().build()));

        webTestClient.post()
                .uri("/consents/revoke")
                .accept(APPLICATION_JSON)
                .contentType(APPLICATION_JSON)
                .header("Authorization", token)
                .bodyValue(revokeRequest)
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    void shouldNotRevokeConsentArtefactWhenItIsNotInGrantedState() throws JsonProcessingException {
        var token = string();
        var scope = "consent.revoke";
        var consentRepresentation = consentRepresentation().status(REVOKED).build();
        var errorResponse = new ErrorRepresentation(new Error(CONSENT_NOT_GRANTED, "Not a granted consent."));
        var errorResponseJson = OBJECT_MAPPER.writeValueAsString(errorResponse);
        var consentRequestId = consentRepresentation.getConsentRequestId();
        List<String> consentIds = new ArrayList<>();
        var consentId = consentRepresentation.getConsentDetail().getConsentId();
        consentIds.add(consentRepresentation.getConsentDetail().getConsentId());
        var revokeRequest = RevokeRequest.builder().consents(consentIds).build();
        var patientId = consentRepresentation.getConsentDetail().getPatient().getId();
        when(pinVerificationTokenService.validateToken(token, scope)).thenReturn(just(new Caller(patientId, false)));
        when(consentArtefactRepository.getConsentWithRequest(eq(consentId))).thenReturn(just(consentRepresentation));
        when(repository.requestOf(consentRequestId, REVOKED.toString(), patientId)).thenReturn(empty());

        webTestClient.post()
                .uri("/consents/revoke")
                .accept(APPLICATION_JSON)
                .contentType(APPLICATION_JSON)
                .header("Authorization", token)
                .bodyValue(revokeRequest)
                .exchange()
                .expectStatus()
                .isEqualTo(412)
                .expectBody()
                .json(errorResponseJson);
        verify(consentArtefactRepository, times(0)).updateStatus(any(), any(), any());
        verifyNoInteractions(consentNotificationPublisher);
    }

    @Test
    void shouldGetAllConsentArtefacts() {
        var token = string();
        var limit = 20;
        List<ConsentArtefactRepresentation> response = new ArrayList<>();
        response.add(consentArtefactRepresentation().build());
        var result = new ListResult<>(response, response.size());
        when(authenticator.verify(token)).thenReturn(just(new Caller("shweta@ncg", true)));
        when(consentArtefactRepository.getAllConsentArtefacts("shweta@ncg", limit, 0, null)).thenReturn(just(result));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/consent-artefacts").queryParam("limit", valueOf(limit)).build())
                .accept(APPLICATION_JSON)
                .header("Authorization", token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ConsentArtefactResponse.class)
                .value(ConsentArtefactResponse::getConsentArtefacts, equalTo(response))
                .value(ConsentArtefactResponse::getLimit, Matchers.is(limit))
                .value(ConsentArtefactResponse::getSize, Matchers.is(1))
                .value(ConsentArtefactResponse::getOffset, Matchers.is(0));
    }

    @Test
    void shouldGetAllConsentArtefactsForStatus() {
        var token = string();
        var limit = 20;
        List<ConsentArtefactRepresentation> response = new ArrayList<>();
        var consentArtefactRepresentation = consentArtefactRepresentation().status(EXPIRED).build();
        response.add(consentArtefactRepresentation);
        var result = new ListResult<>(response, response.size());
        when(authenticator.verify(token)).thenReturn(just(new Caller("shweta@ncg", true)));
        when(consentArtefactRepository.getAllConsentArtefacts("shweta@ncg", limit, 0, "EXPIRED"))
                .thenReturn(just(result));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/consent-artefacts")
                        .queryParam("limit", valueOf(limit))
                        .queryParam("status", "EXPIRED")
                        .build())
                .accept(APPLICATION_JSON)
                .header("Authorization", token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ConsentArtefactResponse.class)
                .value(ConsentArtefactResponse::getConsentArtefacts, equalTo(response))
                .value(ConsentArtefactResponse::getLimit, Matchers.is(limit))
                .value(ConsentArtefactResponse::getSize, Matchers.is(1))
                .value(ConsentArtefactResponse::getOffset, Matchers.is(0));
    }

    static class PropertyInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertyValues values = TestPropertyValues.of(
                    Stream.of("consentmanager.keycloak.baseUrl=" + identityServer.url("")));
            values.applyTo(applicationContext);
        }
    }

    @Test
    void shouldFetchConsent() {
        var token = string();
        var consentArtefact = consentArtefactRepresentation().build();
        var fetchRequest = fetchRequest().consentId(consentArtefact.getConsentDetail().getConsentId())
                .timestamp(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(2))
                .build();
        consentArtefact.getConsentDetail().getPatient().setId("test-user@ncg");
        var caller = ServiceCaller.builder().clientId("Client_ID").roles(of(GATEWAY)).build();
        when(serviceAuthentication.authenticate()).thenReturn(empty());
        when(gatewayTokenVerifier.verify(token)).thenReturn(just(caller));
        when(validator.validate(anyString(), any(LocalDateTime.class))).thenReturn(just(TRUE));
        when(consentArtefactRepository.getConsentArtefact(fetchRequest.getConsentId()))
                .thenReturn(just(consentArtefact));
        when(centralRegistry.providerWith(any())).thenReturn(just(Provider.builder().name("test-hip").build()));
        when(consentManagerClient.sendConsentArtefactResponseToGateway(any(), any())).thenReturn(empty());

        webTestClient.post()
                .uri(Constants.PATH_CONSENTS_FETCH)
                .contentType(APPLICATION_JSON)
                .header("Authorization", token)
                .bodyValue(fetchRequest)
                .exchange()
                .expectStatus()
                .isAccepted();
    }

    @Test
    void shouldCallGatewayIfConsentRequestStatusIsGranted() {
        var token = string();
        var consentRequestStatus = consentRequestStatus().build();
        var hiuId = string();
        var consentArtefactReference = consentArtefactReference().id(string()).build();
        var consentCallerDetails = ConsentStatusCallerDetail.builder().status(GRANTED).hiuId(hiuId).build();

        var caller = ServiceCaller.builder().clientId("Client_ID").roles(of(GATEWAY)).build();
        when(gatewayTokenVerifier.verify(token)).thenReturn(just(caller));
        when(serviceAuthentication.authenticate()).thenReturn(Mono.just(string()));
        when(validator.validate(anyString(), any(LocalDateTime.class))).thenReturn(just(TRUE));
        when(repository.getConsentRequestStatusAndCallerDetails(consentRequestStatus.getConsentRequestId()))
                .thenReturn(Mono.just(consentCallerDetails));
        when(consentArtefactRepository.consentArtefacts(consentRequestStatus.getConsentRequestId()))
                .thenReturn(Flux.just(consentArtefactReference));
        when(consentManagerClient.sendConsentStatusResponseToGateway(any(ConsentStatusResponse.class), eq(hiuId)))
                .thenReturn(Mono.empty());


        webTestClient.post()
                .uri(Constants.CONSENT_REQUESTS_STATUS)
                .contentType(APPLICATION_JSON)
                .header("Authorization", token)
                .bodyValue(consentRequestStatus)
                .exchange()
                .expectStatus()
                .isAccepted();
    }

    @Test
    void shouldCallGatewayIfConsentRequestStatusIsOtherThanGranted() {
        var token = string();
        var consentRequestStatus = consentRequestStatus().build();
        var hiuId = string();
        var consentCallerDetails = ConsentStatusCallerDetail.builder().status(DENIED).hiuId(hiuId).build();

        var caller = ServiceCaller.builder().clientId("Client_ID").roles(of(GATEWAY)).build();
        when(gatewayTokenVerifier.verify(token)).thenReturn(just(caller));
        when(serviceAuthentication.authenticate()).thenReturn(Mono.just(string()));
        when(validator.validate(anyString(), any(LocalDateTime.class))).thenReturn(just(TRUE));
        when(repository.getConsentRequestStatusAndCallerDetails(consentRequestStatus.getConsentRequestId()))
                .thenReturn(Mono.just(consentCallerDetails));
        when(consentManagerClient.sendConsentStatusResponseToGateway(any(ConsentStatusResponse.class), eq(hiuId)))
                .thenReturn(Mono.empty());


        webTestClient.post()
                .uri(Constants.CONSENT_REQUESTS_STATUS)
                .contentType(APPLICATION_JSON)
                .header("Authorization", token)
                .bodyValue(consentRequestStatus)
                .exchange()
                .expectStatus()
                .isAccepted();
    }

    @Test
    void shouldCallGatewayIfConsentRequestIdIsInvalid() {
        var token = string();
        var consentRequestStatus = consentRequestStatus().build();
        var hiuId = "";

        var caller = ServiceCaller.builder().clientId("Client_ID").roles(of(GATEWAY)).build();
        when(gatewayTokenVerifier.verify(token)).thenReturn(just(caller));
        when(serviceAuthentication.authenticate()).thenReturn(Mono.just(string()));
        when(validator.validate(anyString(), any(LocalDateTime.class))).thenReturn(just(TRUE));
        when(repository.getConsentRequestStatusAndCallerDetails(consentRequestStatus.getConsentRequestId()))
                .thenReturn(Mono.empty());
        when(consentManagerClient.sendConsentStatusResponseToGateway(any(ConsentStatusResponse.class), eq(hiuId)))
                .thenReturn(Mono.empty());


        webTestClient.post()
                .uri(Constants.CONSENT_REQUESTS_STATUS)
                .contentType(APPLICATION_JSON)
                .header("Authorization", token)
                .bodyValue(consentRequestStatus)
                .exchange()
                .expectStatus()
                .isAccepted();
    }

    @Test
    void shouldNotifyHiu() {
        String token = string();
        var acknowledgment = hiuConsentNotificationAcknowledgement().build();
        var caller = ServiceCaller.builder().clientId("Client_ID").roles(of(GATEWAY)).build();

        when(gatewayTokenVerifier.verify(token)).thenReturn(just(caller));
        when(serviceAuthentication.authenticate()).thenReturn(Mono.just(string()));
        when(validator.validate(anyString(), any())).thenReturn(just(TRUE));


        webTestClient.post()
                .uri(Constants.PATH_HIU_CONSENT_ON_NOTIFY)
                .contentType(APPLICATION_JSON)
                .header("Authorization",token)
                .bodyValue(acknowledgment)
                .exchange()
                .expectStatus()
                .isAccepted();
    }
}