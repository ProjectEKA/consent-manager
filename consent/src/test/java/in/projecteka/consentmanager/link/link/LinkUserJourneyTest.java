package in.projecteka.consentmanager.link.link;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.consentmanager.clients.LinkServiceClient;
import in.projecteka.consentmanager.clients.model.PatientLinkReferenceResult;
import in.projecteka.consentmanager.clients.model.PatientLinkRequest;
import in.projecteka.consentmanager.clients.model.PatientRepresentation;
import in.projecteka.consentmanager.consent.ConceptValidator;
import in.projecteka.consentmanager.consent.ConsentRequestNotificationListener;
import in.projecteka.consentmanager.consent.HipConsentNotificationListener;
import in.projecteka.consentmanager.consent.HiuConsentNotificationListener;
import in.projecteka.consentmanager.dataflow.DataFlowBroadcastListener;
import in.projecteka.consentmanager.link.Constants;
import in.projecteka.consentmanager.link.discovery.model.patient.response.GatewayResponse;
import in.projecteka.consentmanager.link.link.model.Hip;
import in.projecteka.consentmanager.link.link.model.Links;
import in.projecteka.consentmanager.link.link.model.PatientLinks;
import in.projecteka.consentmanager.link.link.model.PatientLinksResponse;
import in.projecteka.library.clients.model.Error;
import in.projecteka.library.clients.model.ErrorCode;
import in.projecteka.library.clients.model.ErrorRepresentation;
import in.projecteka.library.clients.model.RespError;
import in.projecteka.library.common.Authenticator;
import in.projecteka.library.common.Caller;
import in.projecteka.library.common.GatewayTokenVerifier;
import in.projecteka.library.common.RequestValidator;
import in.projecteka.library.common.ServiceAuthentication;
import in.projecteka.library.common.ServiceCaller;
import in.projecteka.library.common.cache.CacheAdapter;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static in.projecteka.consentmanager.clients.TestBuilders.identifier;
import static in.projecteka.consentmanager.clients.TestBuilders.provider;
import static in.projecteka.consentmanager.common.TestBuilders.OBJECT_MAPPER;
import static in.projecteka.consentmanager.link.Constants.APP_PATH_CONFIRM_LINK;
import static in.projecteka.consentmanager.link.Constants.APP_PATH_LINK_INIT;
import static in.projecteka.consentmanager.link.Constants.HIP_INITIATED_ACTION_LINK;
import static in.projecteka.consentmanager.link.Constants.PATH_HIP_ADD_CONTEXTS;
import static in.projecteka.consentmanager.link.Constants.PATH_LINK_ON_INIT;
import static in.projecteka.consentmanager.link.Constants.USERS_AUTH_CONFIRM;
import static in.projecteka.consentmanager.link.link.TestBuilders.linkHipAction;
import static in.projecteka.consentmanager.link.link.TestBuilders.linkRequest;
import static in.projecteka.consentmanager.link.link.TestBuilders.patientLinkReferenceRequest;
import static in.projecteka.consentmanager.link.link.TestBuilders.patientLinkReferenceResponse;
import static in.projecteka.consentmanager.link.link.TestBuilders.patientLinkReferenceResult;
import static in.projecteka.consentmanager.link.link.TestBuilders.patientLinkRequest;
import static in.projecteka.consentmanager.link.link.TestBuilders.patientRepresentation;
import static in.projecteka.consentmanager.link.link.TestBuilders.string;
import static in.projecteka.consentmanager.link.link.TestBuilders.user;
import static in.projecteka.consentmanager.user.TestBuilders.userAuthConfirmRequest;
import static in.projecteka.library.common.Role.GATEWAY;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.time.LocalDateTime.now;
import static java.time.ZoneOffset.UTC;
import static java.util.List.of;
import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.just;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "6000")
@ContextConfiguration(initializers = LinkUserJourneyTest.ContextInitializer.class)
class LinkUserJourneyTest {
    private static final MockWebServer clientRegistryServer = new MockWebServer();
    private static final MockWebServer hipServer = new MockWebServer();
    private static final MockWebServer userServer = new MockWebServer();
    private static final MockWebServer identityServer = new MockWebServer();
    private static final MockWebServer gatewayServer = new MockWebServer();

    @MockBean
    @Qualifier("linkResults")
    CacheAdapter<String, String> linkResults;

    @MockBean
    @Qualifier("cacheForReplayAttack")
    CacheAdapter<String, String> cacheForReplayAttack;

    @MockBean
    LinkTokenVerifier linkTokenVerifier;

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private LinkRepository linkRepository;

    @MockBean
    private HiuConsentNotificationListener hiuConsentNotificationListener;

    @MockBean
    private HipConsentNotificationListener hipConsentNotificationListener;

    @MockBean
    private DataFlowBroadcastListener dataFlowBroadcastListener;

    @SuppressWarnings("unused")
    @MockBean
    private ConsentRequestNotificationListener consentRequestNotificationListener;

    @MockBean
    private Authenticator authenticator;

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
    private LinkServiceClient linkServiceClient;

    @MockBean
    private GatewayTokenVerifier gatewayTokenVerifier;

    @MockBean
    private ServiceAuthentication serviceAuthentication;

    @AfterAll
    static void tearDown() throws IOException {
        clientRegistryServer.shutdown();
        hipServer.shutdown();
        userServer.shutdown();
        identityServer.shutdown();
        gatewayServer.shutdown();
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void shouldReturnLinkedCareContexts() throws IOException {
        var token = string();
        var patientId = "5@ncg";
        var links = Links.builder()
                .hip(Hip.builder().id("10000004").build())
                .patientRepresentations(patientRepresentation().build()).build();
        List<Links> linksList = new ArrayList<>();
        linksList.add(links);
        var patientLinks =
                PatientLinks.builder()
                        .id(patientId)
                        .links(linksList).build();
        var patientLinksResponse = PatientLinksResponse.builder().patient(patientLinks).build();
        var user = user().build();
        user.setIdentifier(patientId);
        var userJson = OBJECT_MAPPER.writeValueAsString(user);
        clientRegistryServer.setDispatcher(dispatcher);
        userServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody(userJson));
        identityServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody("{}"));
        when(authenticator.verify(token)).thenReturn(just(new Caller(patientId, false)));
        patientLinksResponse.getPatient().setLinks(patientLinks.getLinks().stream()
                .peek(link -> link.setHip(Hip.builder().id(link.getHip().getId()).build()))
                .collect(Collectors.toList()));
        var patientLinksRes = OBJECT_MAPPER.writeValueAsString(patientLinksResponse);
        when(linkRepository.getLinkedCareContextsForAllHip(patientId)).thenReturn(just(patientLinks));

        webTestClient
                .get()
                .uri("/patients/links")
                .accept(APPLICATION_JSON)
                .header("Authorization", token)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .json(patientLinksRes);
    }


    @Test
    void shouldConfirmLinkCareContexts() {
        var token = string();
        when(authenticator.verify(token)).thenReturn(just(new Caller("123@ncg", false)));
        clientRegistryServer.setDispatcher(dispatcher);
        gatewayServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody("{}"));
        PatientLinkRequest patientLinkRequest = patientLinkRequest().build();
        String transactionId = "transactionId";
        String hipId = "10000005";
        when(serviceAuthentication.authenticate()).thenReturn(just(string()));
        when(linkRepository.getTransactionIdFromLinkReference(patientLinkRequest.getLinkRefNumber())).thenReturn(just(transactionId));
        when(linkRepository.getHIPIdFromDiscovery(transactionId)).thenReturn(just(hipId)); //linkRes.getPatient()
        when(linkRepository.insertToLink(eq(hipId), eq("123@ncg"), eq(patientLinkRequest.getLinkRefNumber()), any(), eq(Constants.LINK_INITIATOR_CM)))
                .thenReturn(empty());
        String linkConfirmationResult = "{\n" +
                "  \"requestId\": \"5f7a535d-a3fd-416b-b069-c97d021fbacd\",\n" +
                "  \"timestamp\": \"2020-05-25T15:03:44.557Z\",\n" +
                "  \"patient\": {\n" +
                "    \"referenceNumber\": \"HID-001\",\n" +
                "    \"display\": \"Patient with HID 001\",\n" +
                "    \"careContexts\": [\n" +
                "      {\n" +
                "        \"referenceNumber\": \"CC001\",\n" +
                "        \"display\": \"Episode 001\"\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  \"resp\": {\n" +
                "    \"requestId\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\"\n" +
                "  }\n" +
                "}";
        when(linkResults.get(any())).thenReturn(just(linkConfirmationResult));

        String linkResJson = "{\n" +
                "  \"patient\": {\n" +
                "    \"referenceNumber\": \"HID-001\",\n" +
                "    \"display\": \"Patient with HID 001\",\n" +
                "    \"careContexts\": [\n" +
                "      {\n" +
                "        \"referenceNumber\": \"CC001\",\n" +
                "        \"display\": \"Episode 001\"\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";
        webTestClient
                .post()
                .uri(APP_PATH_CONFIRM_LINK + "/" + patientLinkRequest.getLinkRefNumber())
                .header("Authorization", token)
                .contentType(APPLICATION_JSON)
                .bodyValue(patientLinkRequest)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .json(linkResJson);
    }

    @Test
    void shouldReturnInvalidResponseForConfirmLinkCareContexts() {
        var token = string();
        when(authenticator.verify(token)).thenReturn(just(new Caller("123@ncg", false)));
        clientRegistryServer.setDispatcher(dispatcher);
        gatewayServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody("{}"));
        PatientLinkRequest patientLinkRequest = patientLinkRequest().build();
        String transactionId = "transactionId";
        String hipId = "10000005";
        when(serviceAuthentication.authenticate()).thenReturn(just(string()));
        when(linkRepository.getTransactionIdFromLinkReference(patientLinkRequest.getLinkRefNumber())).thenReturn(just(transactionId));
        when(linkRepository.getHIPIdFromDiscovery(transactionId)).thenReturn(just(hipId)); //linkRes.getPatient()
        when(linkRepository.insertToLink(eq(hipId), eq("123@ncg"), eq(patientLinkRequest.getLinkRefNumber()), any(), eq(Constants.LINK_INITIATOR_CM)))
                .thenReturn(empty());
        String linkConfirmationResult = "{\n" +
                "  \"requestId\": \"5f7a535d-a3fd-416b-b069-c97d021fbacd\",\n" +
                "  \"timestamp\": \"2020-05-25T15:03:44.557Z\",\n" +
                "  \"error\": {\n" +
                "    \"code\": 3413,\n" +
                "    \"message\": \"Invalid Link reference\"\n" +
                "  }," +
                "  \"resp\": {\n" +
                "    \"requestId\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\"\n" +
                "  }\n" +
                "}";
        String errorResponseJson = "{\"error\":{\"code\":1413,\"message\":\"Invalid Link reference\"}}";
        when(linkResults.get(any())).thenReturn(just(linkConfirmationResult));

        webTestClient
                .post()
                .uri(APP_PATH_CONFIRM_LINK + "/" + patientLinkRequest.getLinkRefNumber())
                .header("Authorization", token)
                .contentType(APPLICATION_JSON)
                .bodyValue(patientLinkRequest)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .json(errorResponseJson);
    }

    @Test
    void shouldReturnGatewayTimeOutForConfirmLinkCareContexts() throws IOException {
        var token = string();
        clientRegistryServer.setDispatcher(dispatcher);
        gatewayServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody("{}"));
        PatientLinkRequest patientLinkRequest = patientLinkRequest().build();
        String transactionId = "transactionId";
        String hipId = "10000005";
        when(authenticator.verify(token)).thenReturn(just(new Caller("123@ncg", false)));
        when(serviceAuthentication.authenticate()).thenReturn(just(string()));
        when(linkRepository.getTransactionIdFromLinkReference(patientLinkRequest.getLinkRefNumber()))
                .thenReturn(just(transactionId));
        when(linkRepository.getHIPIdFromDiscovery(transactionId)).thenReturn(just(hipId)); //linkRes.getPatient()
        when(linkResults.get(any())).thenReturn(empty());
        var errorResponse = new ErrorRepresentation(
                new Error(ErrorCode.NO_RESULT_FROM_GATEWAY, "Didn't receive any result from Gateway"));
        var errorResponseJson = OBJECT_MAPPER.writeValueAsString(errorResponse);

        webTestClient
                .post()
                .uri(APP_PATH_CONFIRM_LINK + "/" + patientLinkRequest.getLinkRefNumber())
                .header("Authorization", token)
                .contentType(APPLICATION_JSON)
                .bodyValue(patientLinkRequest)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .is5xxServerError()
                .expectBody()
                .json(errorResponseJson);
    }

    @Test
    void onLinkCareContexts() {
        var token = string();
        var patientLinkReferenceResult = patientLinkReferenceResult()
                .requestId(randomUUID())
                .timestamp(now(UTC).plusSeconds(60L))
                .build();
        var caller = ServiceCaller.builder().clientId("Client_ID").roles(of(GATEWAY)).build();
        when(validator.put(anyString(), any(LocalDateTime.class))).thenReturn(empty());
        when(validator.validate(anyString(), any(LocalDateTime.class))).thenReturn(just(TRUE));
        when(gatewayTokenVerifier.verify(token))
                .thenReturn(just(caller));
        when(cacheForReplayAttack.put(anyString(), anyString())).thenReturn(empty());
        when(linkResults.put(anyString(), anyString())).thenReturn(empty());

        webTestClient.post()
                .uri(PATH_LINK_ON_INIT)
                .accept(APPLICATION_JSON)
                .contentType(APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, token)
                .bodyValue(patientLinkReferenceResult)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void shouldFailWithTwoManyRequestsErrorForInvalidRequest() {
        var token = string();
        var patientLinkReferenceResult = patientLinkReferenceResult()
                .requestId(randomUUID())
                .timestamp(now(UTC))
                .build();
        var caller = ServiceCaller.builder().clientId("Client_ID").roles(of(GATEWAY)).build();
        when(validator.validate(anyString(), any(LocalDateTime.class))).thenReturn(just(FALSE));
        when(gatewayTokenVerifier.verify(token))
                .thenReturn(just(caller));

        webTestClient.post()
                .uri(PATH_LINK_ON_INIT)
                .accept(APPLICATION_JSON)
                .contentType(APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, token)
                .bodyValue(patientLinkReferenceResult)
                .exchange()
                .expectStatus()
                .is4xxClientError();
    }

    @Test
    void shouldFailOnLinkCareContextsWhenRequestIdIsNotGiven() {
        var token = string();
        var gatewayResponse = GatewayResponse.builder().requestId(null).build();
        var patientLinkReferenceResult = PatientLinkReferenceResult.builder()
                .requestId(randomUUID())
                .resp(gatewayResponse)
                .timestamp(now().plusSeconds(60L))
                .build();
        var caller = ServiceCaller.builder().clientId("Client_ID").roles(of(GATEWAY)).build();
        when(validator.put(anyString(), any(LocalDateTime.class))).thenReturn(empty());
        when(validator.validate(anyString(), any(LocalDateTime.class))).thenReturn(just(TRUE));
        when(gatewayTokenVerifier.verify(token)).thenReturn(just(caller));
        when(cacheForReplayAttack.put(anyString(), anyString())).thenReturn(empty());

        webTestClient.post()
                .uri(PATH_LINK_ON_INIT)
                .accept(APPLICATION_JSON)
                .contentType(APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, token)
                .bodyValue(patientLinkReferenceResult)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void shouldFailOnLinkCareContexts() {
        var token = string();
        var caller = ServiceCaller.builder().clientId("Client_ID").roles(of(GATEWAY)).build();
        when(gatewayTokenVerifier.verify(token)).thenReturn(just(caller));
        when(validator.validate(anyString(), any(LocalDateTime.class))).thenReturn(just(TRUE));

        webTestClient.post()
                .uri(PATH_LINK_ON_INIT)
                .accept(APPLICATION_JSON)
                .contentType(APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, token)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void shouldGetPatientLinkReference() throws IOException {
        var token = string();
        var patientLinkReferenceRequest = patientLinkReferenceRequest().build();
        var linkReferenceRequest = TestBuilders.linkReferenceRequest().build();
        var hipId = "10000005";
        var patientLinkReferenceResult = patientLinkReferenceResult().error(null).build();
        String patientLinkReferenceResultJson = OBJECT_MAPPER.writeValueAsString(patientLinkReferenceResult);
        var linkReferenceResponse = patientLinkReferenceResponse()
                .transactionId(patientLinkReferenceResult.getTransactionId().toString())
                .link(patientLinkReferenceResult.getLink())
                .build();
        String linkReferenceResponseJson = OBJECT_MAPPER.writeValueAsString(linkReferenceResponse);
        when(serviceAuthentication.authenticate()).thenReturn(just(string()));
        when(authenticator.verify(token)).thenReturn(just(new Caller("user-id", false)));
        gatewayServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody("{}"));
        clientRegistryServer.setDispatcher(dispatcher);
        when(linkRepository.getHIPIdFromDiscovery(patientLinkReferenceRequest.getTransactionId()))
                .thenReturn(just(hipId));
        when(linkRepository.insert(patientLinkReferenceResult, hipId, patientLinkReferenceRequest.getRequestId()))
                .thenReturn(Mono.create(MonoSink::success));
        when(linkRepository.selectLinkReference(patientLinkReferenceRequest.getRequestId()))
                .thenReturn(empty());
        when(linkServiceClient.linkPatientEnquiryRequest(linkReferenceRequest, token, hipId)).thenReturn(just(true));
        when(linkResults.get(any())).thenReturn(just(patientLinkReferenceResultJson));

        webTestClient
                .post()
                .uri(APP_PATH_LINK_INIT)
                .header("Authorization", token)
                .contentType(APPLICATION_JSON)
                .bodyValue(patientLinkReferenceRequest)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .json(linkReferenceResponseJson);
    }

    @Test
    void shouldFailPatientLinkReference() throws IOException {
        var token = string();
        var patientLinkReferenceRequest = patientLinkReferenceRequest().build();
        var linkReferenceRequest = TestBuilders.linkReferenceRequest().build();
        var hipId = "10000005";
        var patientLinkReferenceResult = patientLinkReferenceResult().error(RespError.builder().build()).build();
        String patientLinkReferenceResultJson = OBJECT_MAPPER.writeValueAsString(patientLinkReferenceResult);
        when(authenticator.verify(token)).thenReturn(just(new Caller("user-id", false)));
        gatewayServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody("{}"));
        clientRegistryServer.setDispatcher(dispatcher);
        when(linkRepository.getHIPIdFromDiscovery(patientLinkReferenceRequest.getTransactionId()))
                .thenReturn(just(hipId));
        when(linkRepository.selectLinkReference(patientLinkReferenceRequest.getRequestId()))
                .thenReturn(empty());
        when(linkServiceClient.linkPatientEnquiryRequest(linkReferenceRequest, token, hipId))
                .thenReturn(just(true));
        when(linkResults.get(any())).thenReturn(just(patientLinkReferenceResultJson));
        when(serviceAuthentication.authenticate()).thenReturn(just(string()));

        webTestClient
                .post()
                .uri(APP_PATH_LINK_INIT)
                .header("Authorization", token)
                .contentType(APPLICATION_JSON)
                .bodyValue(patientLinkReferenceRequest)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    void shouldLinkCareContexts() {
        var token = string();
        var linkRequest = linkRequest().build();
        var hipAction = linkHipAction().build();
        var caller = ServiceCaller.builder().clientId("Client_ID").roles(of(GATEWAY)).build();
        when(gatewayTokenVerifier.verify(token)).thenReturn(just(caller));
        clientRegistryServer.setDispatcher(dispatcher);
        gatewayServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody("{}"));
        when(validator.validate(anyString(), any(LocalDateTime.class))).thenReturn(just(TRUE));
        when(linkTokenVerifier.getHipIdFromToken(linkRequest.getLink().getAccessToken())).thenReturn(Mono.just(hipAction.getHipId()));
        when(linkTokenVerifier.validateSession(linkRequest.getLink().getAccessToken())).thenReturn(Mono.just(hipAction));
        when(linkTokenVerifier.validateHipAction(hipAction, HIP_INITIATED_ACTION_LINK)).thenReturn(Mono.just(hipAction));
        when(linkRepository.insertToLink(eq(hipAction.getHipId()), eq(hipAction.getPatientId()), eq(hipAction.getSessionId()),
                any(PatientRepresentation.class), eq(Constants.LINK_INITIATOR_HIP))).thenReturn(Mono.empty());
        when(linkRepository.incrementHipActionCounter(hipAction.getSessionId())).thenReturn(Mono.empty());
        when(linkServiceClient.sendLinkResponseToGateway(any(), anyString())).thenReturn(Mono.empty());
        when(serviceAuthentication.authenticate()).thenReturn(just(string()));

        webTestClient
                .post()
                .uri(PATH_HIP_ADD_CONTEXTS)
                .header("Authorization", token)
                .contentType(APPLICATION_JSON)
                .bodyValue(linkRequest)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isAccepted();
    }

    static class ContextInitializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertyValues values =
                    TestPropertyValues.of(
                            Stream.of("consentmanager.clientregistry.url=" + clientRegistryServer.url(""),
                                    "consentmanager.userservice.url=" + userServer.url(""),
                                    "consentmanager.keycloak.baseUrl=" + identityServer.url(""),
                                    "consentmanager.gatewayservice.baseUrl=" + gatewayServer.url("")));
            values.applyTo(applicationContext);
        }
    }

    @Test
    void shouldReturnAcceptedForUserAuthConfirmRequest() {
        var token = in.projecteka.consentmanager.user.TestBuilders.string();
        var userAuthConfirmRequest = userAuthConfirmRequest().build();
        var caller = ServiceCaller.builder().clientId("Client_ID").roles(List.of(GATEWAY)).build();
        when(validator.put(anyString(), any(LocalDateTime.class))).thenReturn(Mono.empty());
        when(validator.validate(anyString(), any(LocalDateTime.class))).thenReturn(Mono.just(Boolean.TRUE));
        when(gatewayTokenVerifier.verify(token)).thenReturn(just(caller));

        webTestClient.post()
                .uri(USERS_AUTH_CONFIRM)
                .accept(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .body(BodyInserters.fromValue(userAuthConfirmRequest))
                .exchange()
                .expectStatus()
                .isAccepted();
    }

    @Test
    void shouldThrowTooManyRequestErrorForInvalidAuthConfirmRequest() {
        var token = in.projecteka.consentmanager.user.TestBuilders.string();
        var caller = ServiceCaller.builder().clientId("Client_ID").roles(List.of(GATEWAY)).build();
        var userAuthConfirmRequest = userAuthConfirmRequest().build();
        when(validator.validate(anyString(), any(LocalDateTime.class))).thenReturn(Mono.empty());
        when(gatewayTokenVerifier.verify(token)).thenReturn(just(caller));

        webTestClient.post()
                .uri(USERS_AUTH_CONFIRM)
                .accept(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .bodyValue(userAuthConfirmRequest)
                .exchange()
                .expectStatus()
                .is4xxClientError();
    }

    final Dispatcher dispatcher = new Dispatcher() {
        @Override
        public MockResponse dispatch(RecordedRequest request) {
            var official = identifier().use("official").system(hipServer.url("").toString()).build();
            var maxProvider = provider().name("Max").identifiers(of(official)).build();
            var tmhProvider = provider().name("TMH").identifiers(of(official)).build();
            String mxAsJson = null;
            String tmhJson = null;
            try {
                mxAsJson = OBJECT_MAPPER.writeValueAsString(maxProvider);
                tmhJson = OBJECT_MAPPER.writeValueAsString(tmhProvider);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            switch (request.getPath()) {
                case "/api/2.0/providers/10000004":
                    return new MockResponse().setResponseCode(200).setBody(mxAsJson).setHeader("content-type",
                            "application/json");
                case "/api/2.0/providers/10000005":
                    return new MockResponse().setResponseCode(200).setBody(tmhJson).setHeader("content-type",
                            "application/json");
            }
            return new MockResponse().setResponseCode(404);
        }
    };
}
