package in.projecteka.consentmanager.link.link;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.consent.ConsentArtefactBroadcastListener;
import in.projecteka.consentmanager.consent.ConsentRequestNotificationListener;
import in.projecteka.consentmanager.dataflow.DataFlowBroadcastListener;
import in.projecteka.consentmanager.link.TestBuilders;
import in.projecteka.consentmanager.link.link.model.Error;
import in.projecteka.consentmanager.link.link.model.ErrorCode;
import in.projecteka.consentmanager.link.link.model.ErrorRepresentation;
import in.projecteka.consentmanager.link.link.model.Hip;
import in.projecteka.consentmanager.link.link.model.Links;
import in.projecteka.consentmanager.link.link.model.PatientLinkRequest;
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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static in.projecteka.consentmanager.link.link.TestBuilders.identifier;
import static in.projecteka.consentmanager.link.link.TestBuilders.patientLinkReferenceRequest;
import static in.projecteka.consentmanager.link.link.TestBuilders.patientLinkReferenceResponse;
import static in.projecteka.consentmanager.link.link.TestBuilders.patientLinkRequest;
import static in.projecteka.consentmanager.link.link.TestBuilders.patientRepresentation;
import static in.projecteka.consentmanager.link.link.TestBuilders.provider;
import static in.projecteka.consentmanager.link.link.TestBuilders.user;
import static java.util.List.of;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ContextConfiguration(initializers = LinkUserJourneyTest.ContextInitializer.class)
public class LinkUserJourneyTest {
    private static MockWebServer clientRegistryServer = new MockWebServer();
    private static MockWebServer hipServer = new MockWebServer();
    private static MockWebServer userServer = new MockWebServer();
    private static MockWebServer identityServer = new MockWebServer();

    @MockBean
    private DestinationsConfig destinationsConfig;

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private LinkRepository linkRepository;

    @MockBean
    private ConsentArtefactBroadcastListener consentArtefactBroadcastListener;

    @MockBean
    private DataFlowBroadcastListener dataFlowBroadcastListener;

    @MockBean
    private ConsentRequestNotificationListener consentRequestNotificationListener;

    @AfterAll
    public static void tearDown() throws IOException {
        clientRegistryServer.shutdown();
        hipServer.shutdown();
        userServer.shutdown();
        identityServer.shutdown();
    }

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldGetLinkReference() throws IOException {
        var patientLinkReferenceRequest = patientLinkReferenceRequest().build();
        var hipId = "10000005";
        var linkReference = patientLinkReferenceResponse().build();
        linkReference.setTransactionId(patientLinkReferenceRequest.getTransactionId());
        var linkReferenceJson = new ObjectMapper().writeValueAsString(linkReference);
        var user = "{\"preferred_username\": \"user-id\"}";
        identityServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody(user));
        identityServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody(user));
        hipServer.enqueue(
                new MockResponse()
                        .setHeader("Content-Type", "application/json")
                        .setBody(linkReferenceJson));
        clientRegistryServer.setDispatcher(dispatcher);
        when(linkRepository.getHIPIdFromDiscovery(patientLinkReferenceRequest.getTransactionId()))
                .thenReturn(Mono.just(hipId));
        when(linkRepository.insertToLinkReference(linkReference, hipId)).thenReturn(Mono.create(MonoSink::success));

        webTestClient
                .post()
                .uri("/patients/link")
                .header("Authorization", "MTIzNDU2Nzg5")
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
        var errorResponse = TestBuilders.errorRepresentation().build();
        var errorResponseJson = new ObjectMapper().writeValueAsString(errorResponse);
        clientRegistryServer.setDispatcher(dispatcher);
        hipServer.enqueue(
                new MockResponse()
                        .setHeader("Content-Type", "application/json")
                        .setStatus("HTTP/1.1 404")
                        .setBody(errorResponseJson));
        var patientLinkReferenceRequest = patientLinkReferenceRequest().build();
        var hipId = "10000005";
        var user = "{\"preferred_username\": \"user-id\"}";
        identityServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody(user));
        identityServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody(user));
        when(linkRepository.getHIPIdFromDiscovery(patientLinkReferenceRequest.getTransactionId()))
                .thenReturn(Mono.just(hipId));

        webTestClient
                .post()
                .uri("/patients/link")
                .header("Authorization", "MTIzNDU2Nzg5")
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
        var linkRes = TestBuilders.patientLinkResponse().build();
        var linkResJson = new ObjectMapper().writeValueAsString(linkRes);
        var user = "{\"preferred_username\": \"123@ncg\"}";
        identityServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody(user));
        identityServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody(user));
        clientRegistryServer.setDispatcher(dispatcher);
        hipServer.enqueue(
                new MockResponse()
                        .setHeader("Content-Type", "application/json")
                        .setBody(linkResJson));
        PatientLinkRequest patientLinkRequest = patientLinkRequest().build();
        String transactionId = "transactionId";
        String hipId = "10000005";
        String linkRefNumber = "link-ref-num";
        ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneOffset.UTC).withNano(0).plusHours(1);
        when(linkRepository.getExpiryFromLinkReference(linkRefNumber)).thenReturn(Mono.just(zonedDateTime.toString()));
        when(linkRepository.getTransactionIdFromLinkReference(linkRefNumber)).thenReturn(Mono.just(transactionId));
        when(linkRepository.getHIPIdFromDiscovery(transactionId)).thenReturn(Mono.just(hipId));
        when(linkRepository.insertToLink(hipId, "123@ncg", linkRefNumber, linkRes.getPatient()))
                .thenReturn(Mono.empty());

        webTestClient
                .post()
                .uri("/patients/link/link-ref-num")
                .header("Authorization", "MTIzNDU2Nzg5")
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
        var errorResponse = new ErrorRepresentation(new Error(ErrorCode.OTP_EXPIRED, "OTP Expired, please try again"));
        var errorResponseJson = new ObjectMapper().writeValueAsString(errorResponse);
        PatientLinkRequest patientLinkRequest = patientLinkRequest().build();
        String transactionId = "transactionId";
        String hipId = "10000005";
        String linkRefNumber = "link-ref";
        ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneOffset.UTC).withNano(0);
        when(linkRepository.getExpiryFromLinkReference(linkRefNumber)).thenReturn(Mono.just(zonedDateTime.toString()));
        when(linkRepository.getTransactionIdFromLinkReference(linkRefNumber))
                .thenReturn(Mono.just(transactionId));
        when(linkRepository.getHIPIdFromDiscovery(transactionId)).thenReturn(Mono.just(hipId));
        clientRegistryServer.setDispatcher(dispatcher);
        var user = "{\"preferred_username\": \"123@ncg\"}";
        identityServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody(user));
        identityServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody(user));

        webTestClient
                .post()
                .uri("/patients/link/link-ref")
                .header("Authorization", "MTIzNDU2Nzg5")
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
        var patientId = "5@ncg";
        var links = Links.builder()
                .hip(Hip.builder().id("10000004").name("").build())
                .patientRepresentations(patientRepresentation().build()).build();
        List<Links> linksList = new ArrayList<>();
        linksList.add(links);
        var patientLinks =
                PatientLinks.builder().
                        id(patientId).
                        firstName("").
                        lastName("").
                        links(linksList).build();
        var patientLinksResponse = PatientLinksResponse.builder().patient(patientLinks).build();
        var user = user().build();
        user.setIdentifier(patientId);
        var userJson = new ObjectMapper().writeValueAsString(user);
        clientRegistryServer.setDispatcher(dispatcher);
        userServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody(userJson));
        var patient = "{\"preferred_username\": \"5@ncg\"}";
        identityServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody(patient));
        identityServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody(patient));

        patientLinksResponse.getPatient().setFirstName(user.getFirstName());
        patientLinksResponse.getPatient().setLastName(user.getLastName());
        patientLinksResponse.getPatient().setLinks(patientLinks.getLinks().stream()
                .peek(link -> link.setHip(Hip.builder().id(link.getHip().getId()).name("Max").build()))
                .collect(Collectors.toList()));
        var patientLinksRes = new ObjectMapper().writeValueAsString(patientLinksResponse);
        when(linkRepository.getLinkedCareContextsForAllHip(patientId)).thenReturn(Mono.just(patientLinks));

        webTestClient
                .get()
                .uri("/patients/links")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", patient)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .json(patientLinksRes);
    }

    public static class ContextInitializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertyValues values =
                    TestPropertyValues.of(
                            Stream.of("consentmanager.clientregistry.url=" + clientRegistryServer.url(""),
                                    "consentmanager.userservice.url=" + userServer.url(""),
                                    "consentmanager.keycloak.baseUrl=" + identityServer.url("")));
            values.applyTo(applicationContext);
        }
    }

    final Dispatcher dispatcher = new Dispatcher() {
        @Override
        public MockResponse dispatch (RecordedRequest request) {
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
                case "/providers/10000004":
                    return new MockResponse().setResponseCode(200).setBody(mxAsJson).setHeader("content-type",
                            "application/json");
                case "/providers/10000005":
                    return new MockResponse().setResponseCode(200).setBody(tmhJson).setHeader("content-type",
                            "application/json");
            }
            return new MockResponse().setResponseCode(404);
        }
    };
}
