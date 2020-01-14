package in.projecteka.consentmanager.link.link;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.consentmanager.link.TestBuilders;
import in.projecteka.consentmanager.link.link.model.Error;
import in.projecteka.consentmanager.link.link.model.ErrorCode;
import in.projecteka.consentmanager.link.link.model.ErrorRepresentation;
import in.projecteka.consentmanager.link.link.model.PatientLinkReferenceRequest;
import in.projecteka.consentmanager.link.link.model.PatientLinkRequest;
import in.projecteka.consentmanager.link.link.repository.LinkRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
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
import java.util.stream.Stream;

import static in.projecteka.consentmanager.link.link.TestBuilders.*;
import static java.util.List.of;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ContextConfiguration(initializers = LinkUserJourneyTest.ContextInitializer.class)
public class LinkUserJourneyTest {
    private static MockWebServer clientRegistryServer = new MockWebServer();
    private static MockWebServer hipServer = new MockWebServer();

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private LinkRepository linkRepository;

    @AfterAll
    public static void tearDown() throws IOException {
        clientRegistryServer.shutdown();
        hipServer.shutdown();
    }

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldGetLinkReference() throws IOException {
        var official = identifier().use("official").system(hipServer.url("").toString()).build();
        var provider = provider().identifiers(of(official)).build();
        var providerAsJson = new ObjectMapper().writeValueAsString(provider);
        clientRegistryServer.enqueue(
                new MockResponse().setHeader("Content-Type", "application/json").setBody(providerAsJson));

        PatientLinkReferenceRequest patientLinkReferenceRequest = patientLinkReferenceRequest().build();
        String hipId = "10000005";
        var linkReference = patientLinkReferenceResponse().build();
        linkReference.setTransactionId(patientLinkReferenceRequest.getTransactionId());
        var linkReferenceJson = new ObjectMapper().writeValueAsString(linkReference);
        hipServer.enqueue(
                new MockResponse()
                        .setHeader("Content-Type", "application/json")
                        .setBody(linkReferenceJson));

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
        var official = identifier().use("official").system(hipServer.url("").toString()).build();
        var provider = provider().identifiers(of(official)).build();
        var providerAsJson = new ObjectMapper().writeValueAsString(provider);
        clientRegistryServer.enqueue(
                new MockResponse().setHeader("Content-Type", "application/json").setBody(providerAsJson));

        var errorResponse = TestBuilders.errorRepresentation().build();
        var errorResponseJson = new ObjectMapper().writeValueAsString(errorResponse);
        hipServer.enqueue(
                new MockResponse()
                        .setHeader("Content-Type", "application/json")
                        .setStatus("HTTP/1.1 404")
                        .setBody(errorResponseJson));
        PatientLinkReferenceRequest patientLinkReferenceRequest = patientLinkReferenceRequest().build();
        String hipId = "10000005";

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
        var official = TestBuilders.identifier().use("official").system(hipServer.url("").toString()).build();
        var provider = TestBuilders.provider().identifiers(of(official)).build();
        var providerAsJson = new ObjectMapper().writeValueAsString(provider);
        clientRegistryServer.enqueue(
                new MockResponse().setHeader("Content-Type", "application/json").setBody(providerAsJson));

        var linkRes = TestBuilders.patientLinkResponse().build();
        var linkResJson = new ObjectMapper().writeValueAsString(linkRes);
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
        when(linkRepository.insertToLink(hipId, "123456789", linkRefNumber, linkRes.getPatient()))
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
        var official = identifier().use("official").system(hipServer.url("").toString()).build();
        var provider = provider().identifiers(of(official)).build();
        var providerAsJson = new ObjectMapper().writeValueAsString(provider);
        clientRegistryServer.enqueue(
                new MockResponse().setHeader("Content-Type", "application/json").setBody(providerAsJson));

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

    public static class ContextInitializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertyValues values =
                    TestPropertyValues.of(
                            Stream.of("consentmanager.clientregistry.url=" + clientRegistryServer.url("")));
            values.applyTo(applicationContext);
        }
    }
}
