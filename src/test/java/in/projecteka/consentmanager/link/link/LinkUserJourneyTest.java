package in.projecteka.consentmanager.link.link;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.clients.model.Error;
import in.projecteka.consentmanager.clients.model.ErrorCode;
import in.projecteka.consentmanager.clients.model.ErrorRepresentation;
import in.projecteka.consentmanager.clients.model.PatientLinkRequest;
import in.projecteka.consentmanager.common.Authenticator;
import in.projecteka.consentmanager.common.Caller;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import in.projecteka.consentmanager.consent.ConceptValidator;
import in.projecteka.consentmanager.consent.ConsentRequestNotificationListener;
import in.projecteka.consentmanager.consent.HipConsentNotificationListener;
import in.projecteka.consentmanager.consent.HiuConsentNotificationListener;
import in.projecteka.consentmanager.dataflow.DataFlowBroadcastListener;
import in.projecteka.consentmanager.link.link.model.Hip;
import in.projecteka.consentmanager.link.link.model.Links;
import in.projecteka.consentmanager.link.link.model.PatientLinks;
import in.projecteka.consentmanager.link.link.model.PatientLinksResponse;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static in.projecteka.consentmanager.link.link.TestBuilders.errorRepresentation;
import static in.projecteka.consentmanager.link.link.TestBuilders.identifier;
import static in.projecteka.consentmanager.link.link.TestBuilders.patientLinkReferenceRequest;
import static in.projecteka.consentmanager.link.link.TestBuilders.patientLinkReferenceResponse;
import static in.projecteka.consentmanager.link.link.TestBuilders.patientLinkRequest;
import static in.projecteka.consentmanager.link.link.TestBuilders.patientLinkResponse;
import static in.projecteka.consentmanager.link.link.TestBuilders.patientRepresentation;
import static in.projecteka.consentmanager.link.link.TestBuilders.provider;
import static in.projecteka.consentmanager.link.link.TestBuilders.string;
import static in.projecteka.consentmanager.link.link.TestBuilders.user;
import static java.util.List.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "6000")
@ContextConfiguration(initializers = LinkUserJourneyTest.ContextInitializer.class)
public class LinkUserJourneyTest {
    private static final MockWebServer clientRegistryServer = new MockWebServer();
    private static final MockWebServer hipServer = new MockWebServer();
    private static final MockWebServer userServer = new MockWebServer();
    private static final MockWebServer identityServer = new MockWebServer();
    private static final MockWebServer gatewayServer = new MockWebServer();

    @MockBean
    private DestinationsConfig destinationsConfig;

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
    @MockBean(name = "centralRegistryJWKSet")
    private JWKSet centralRegistryJWKSet;

    @SuppressWarnings("unused")
    @MockBean(name = "identityServiceJWKSet")
    private JWKSet identityServiceJWKSet;

    @SuppressWarnings("unused")
    @MockBean
    private ConceptValidator conceptValidator;

    @MockBean
    @Qualifier("linkResults")
    CacheAdapter<String,String> linkResults;

    @AfterAll
    public static void tearDown() throws IOException {
        clientRegistryServer.shutdown();
        hipServer.shutdown();
        userServer.shutdown();
        identityServer.shutdown();
        gatewayServer.shutdown();
    }

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldGetLinkReference() throws IOException {
        var token = string();
        var patientLinkReferenceRequest = patientLinkReferenceRequest().build();
        var hipId = "10000005";
        var linkReference = patientLinkReferenceResponse().build();
        linkReference.setTransactionId(patientLinkReferenceRequest.getTransactionId());
        var linkReferenceJson = new ObjectMapper().writeValueAsString(linkReference);
        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller("user-id", false)));
        hipServer.enqueue(
                new MockResponse()
                        .setHeader("Content-Type", "application/json")
                        .setBody(linkReferenceJson));
        clientRegistryServer.setDispatcher(dispatcher);
        when(linkRepository.getHIPIdFromDiscovery(patientLinkReferenceRequest.getTransactionId()))
                .thenReturn(Mono.just(hipId));
        when(linkRepository.insertToLinkReference(linkReference, hipId, patientLinkReferenceRequest.getRequestId()))
                .thenReturn(Mono.create(MonoSink::success));
        when(linkRepository.selectLinkReference(patientLinkReferenceRequest.getRequestId()))
                .thenReturn(Mono.empty());


        webTestClient
                .post()
                .uri("/patients/link")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(patientLinkReferenceRequest)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .json(linkReferenceJson);
    }

    @Test
    public void shouldGiveErrorFromHIP() throws IOException {
        var token = string();
        var errorResponse = errorRepresentation().build();
        var errorResponseJson = new ObjectMapper().writeValueAsString(errorResponse);
        clientRegistryServer.setDispatcher(dispatcher);
        hipServer.enqueue(
                new MockResponse()
                        .setHeader("Content-Type", "application/json")
                        .setStatus("HTTP/1.1 404")
                        .setBody(errorResponseJson));
        var patientLinkReferenceRequest = patientLinkReferenceRequest().build();
        var hipId = "10000005";
        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller("user-id", false)));
        when(linkRepository.getHIPIdFromDiscovery(patientLinkReferenceRequest.getTransactionId()))
                .thenReturn(Mono.just(hipId));
        when(linkRepository.selectLinkReference(patientLinkReferenceRequest.getRequestId()))
                .thenReturn(Mono.empty());

        webTestClient
                .post()
                .uri("/patients/link")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(patientLinkReferenceRequest)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .json(errorResponseJson);
    }

    @Test
    public void shouldLinkCareContexts() throws IOException {
        var token = string();
        var linkRes = patientLinkResponse().build();
        var linkResJson = new ObjectMapper().writeValueAsString(linkRes);
        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller("123@ncg", false)));
        clientRegistryServer.setDispatcher(dispatcher);
        hipServer.enqueue(
                new MockResponse()
                        .setHeader("Content-Type", "application/json")
                        .setBody(linkResJson));
        PatientLinkRequest patientLinkRequest = patientLinkRequest().build();
        String transactionId = "transactionId";
        String hipId = "10000005";
        String linkRefNumber = "link-ref-num";
        when(linkRepository.getTransactionIdFromLinkReference(linkRefNumber)).thenReturn(Mono.just(transactionId));
        when(linkRepository.getHIPIdFromDiscovery(transactionId)).thenReturn(Mono.just(hipId));
        when(linkRepository.insertToLink(hipId, "123@ncg", linkRefNumber, linkRes.getPatient()))
                .thenReturn(Mono.empty());

        webTestClient
                .post()
                .uri("/patients/link/link-ref-num")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(patientLinkRequest)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .json(linkResJson);
    }

    @Test
    public void shouldGiveOtpExpiredError() throws IOException {
        var token = string();
        var errorResponse = new ErrorRepresentation(new Error(ErrorCode.OTP_EXPIRED, "OTP Expired, please try again"));
        var errorResponseJson = new ObjectMapper().writeValueAsString(errorResponse);
        clientRegistryServer.setDispatcher(dispatcher);
        hipServer.enqueue(
                new MockResponse()
                        .setHeader("Content-Type", "application/json")
                        .setStatus("HTTP/1.1 401")
                        .setBody(errorResponseJson));
        PatientLinkRequest patientLinkRequest = patientLinkRequest().build();
        String transactionId = "transactionId";
        String hipId = "10000005";
        String linkRefNumber = "link-ref-num";
        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller("123@ncg", false)));
        when(linkRepository.getTransactionIdFromLinkReference(linkRefNumber)).thenReturn(Mono.just(transactionId));
        when(linkRepository.getHIPIdFromDiscovery(transactionId)).thenReturn(Mono.just(hipId));

        webTestClient
                .post()
                .uri("/patients/link/link-ref-num")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(patientLinkRequest)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody()
                .json(errorResponseJson);
    }

    @Test
    public void shouldReturnLinkedCareContexts() throws IOException {
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
        var userJson = new ObjectMapper().writeValueAsString(user);
        clientRegistryServer.setDispatcher(dispatcher);
        userServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody(userJson));
        identityServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody("{}"));
        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller(patientId, false)));
        patientLinksResponse.getPatient().setLinks(patientLinks.getLinks().stream()
                .peek(link -> link.setHip(Hip.builder().id(link.getHip().getId()).build()))
                .collect(Collectors.toList()));
        var patientLinksRes = new ObjectMapper().writeValueAsString(patientLinksResponse);
        when(linkRepository.getLinkedCareContextsForAllHip(patientId)).thenReturn(Mono.just(patientLinks));

        webTestClient
                .get()
                .uri("/patients/links")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", token)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .json(patientLinksRes);
    }

    @Test
    public void shouldGiveRequestAlreadyExistsError() throws IOException {
        var token = string();
        var errorResponse = ErrorRepresentation.builder()
                .error(Error.builder()
                        .code(ErrorCode.REQUEST_ALREADY_EXISTS)
                        .message("A request with this request id already exists.")
                        .build())
                .build();
        var errorResponseJson = new ObjectMapper().writeValueAsString(errorResponse);
        clientRegistryServer.setDispatcher(dispatcher);
        var patientLinkReferenceRequest = patientLinkReferenceRequest().build();

        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller("user-id", false)));
        when(linkRepository.selectLinkReference(patientLinkReferenceRequest.getRequestId()))
                .thenReturn(Mono.just("some string"));

        webTestClient
                .post()
                .uri("/patients/link")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(patientLinkReferenceRequest)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .json(errorResponseJson);
    }

    @Test
    public void shouldConfirmLinkCareContexts() throws IOException {
        var token = string();
        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller("123@ncg", false)));
        clientRegistryServer.setDispatcher(dispatcher);
        gatewayServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody("{}"));
        PatientLinkRequest patientLinkRequest = patientLinkRequest().build();
        String transactionId = "transactionId";
        String hipId = "10000005";
        when(linkRepository.getTransactionIdFromLinkReference(patientLinkRequest.getLinkRefNumber())).thenReturn(Mono.just(transactionId));
        when(linkRepository.getHIPIdFromDiscovery(transactionId)).thenReturn(Mono.just(hipId)); //linkRes.getPatient()
        when(linkRepository.insertToLink(eq(hipId), eq("123@ncg"), eq(patientLinkRequest.getLinkRefNumber()), any()))
                .thenReturn(Mono.empty());
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
        when(linkResults.get(any())).thenReturn(Mono.just(linkConfirmationResult));

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
                .uri("/v1/links/link/confirm")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(patientLinkRequest)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .json(linkResJson);
    }

    public static class ContextInitializer
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

    final Dispatcher dispatcher = new Dispatcher() {
        @Override
        public MockResponse dispatch(RecordedRequest request) {
            var session = "{\"accessToken\": \"eyJhbGc\", \"refreshToken\": \"eyJhbGc\"}";
            var official = identifier().use("official").system(hipServer.url("").toString()).build();
            var maxProvider = provider().name("Max").identifiers(of(official)).build();
            var tmhProvider = provider().name("TMH").identifiers(of(official)).build();
            String mxAsJson = null;
            String tmhJson = null;
            try {
                mxAsJson = new ObjectMapper().writeValueAsString(maxProvider);
                tmhJson = new ObjectMapper().writeValueAsString(tmhProvider);
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
                case "/api/1.0/sessions":
                    return new MockResponse().setResponseCode(200).setBody(session).setHeader("content-type",
                            "application/json");
            }
            return new MockResponse().setResponseCode(404);
        }
    };
}
