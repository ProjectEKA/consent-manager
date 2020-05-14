package in.projecteka.consentmanager.link.discovery;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.clients.DiscoveryServiceClient;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.common.Authenticator;
import in.projecteka.consentmanager.common.Caller;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import in.projecteka.consentmanager.consent.ConsentRequestNotificationListener;
import in.projecteka.consentmanager.consent.HipConsentNotificationListener;
import in.projecteka.consentmanager.consent.HiuConsentNotificationListener;
import in.projecteka.consentmanager.dataflow.DataFlowBroadcastListener;
import in.projecteka.consentmanager.link.discovery.model.patient.response.PatientResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
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
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static in.projecteka.consentmanager.link.discovery.TestBuilders.string;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;


@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "6000000")
@ContextConfiguration(initializers = DiscoveryUserJourneyTest.ContextInitializer.class)
public class DiscoveryUserJourneyTest {

    @SuppressWarnings("unused")
    @MockBean
    private DestinationsConfig destinationsConfig;

    @SuppressWarnings("unused")
    @MockBean
    private HiuConsentNotificationListener hiuConsentNotificationListener;

    @SuppressWarnings("unused")
    @MockBean
    private HipConsentNotificationListener hipConsentNotificationListener;

    @SuppressWarnings("unused")
    @MockBean
    private DataFlowBroadcastListener dataFlowBroadcastListener;

    @SuppressWarnings("unused")
    @MockBean
    private ConsentRequestNotificationListener consentRequestNotificationListener;
    private static final MockWebServer providerServer = new MockWebServer();

    @Autowired
    private WebTestClient webTestClient;

    @SuppressWarnings("unused")
    @MockBean(name = "centralRegistryJWKSet")
    private JWKSet centralRegistryJWKSet;

    @SuppressWarnings("unused")
    @MockBean(name = "identityServiceJWKSet")
    private JWKSet identityServiceJWKSet;

    @MockBean
    private Authenticator authenticator;

    @MockBean
    private UserServiceClient userServiceClient;

    @MockBean
    private DiscoveryRepository discoveryRepository;

    @MockBean
    private DiscoveryServiceClient discoveryServiceClient;

    @MockBean
    @Qualifier("discoveryResults")
    CacheAdapter<String,String> discoveryResults;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @AfterAll
    public static void tearDown() throws IOException {
        providerServer.shutdown();
    }

    @Test
    public void shouldGetProvidersByName() throws IOException {
        var providers = new ObjectMapper().readValue(
                Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResource("provider.json")),
                new TypeReference<List<JsonNode>>() {
                });
        var token = string();
        var session = "{\"accessToken\": \"eyJhbGc\"}";
        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller(
                "consent-manager-service", true)));
        providerServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(session));
        providerServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(providers.toString()));

        webTestClient.get()
                .uri("/providers?name=Max")
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.[0].identifier.name").isEqualTo("Max Health Care")
                .jsonPath("$.[0].identifier.id").isEqualTo("12345")
                .jsonPath("$.[0].city").isEqualTo("Bangalore")
                .jsonPath("$.[0].telephone").isEqualTo("08080887876")
                .jsonPath("$.[0].type").isEqualTo("prov");
    }

    @Test
    public void shouldGetProviderById() throws IOException {
        var providers = new ObjectMapper().readValue(
                Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResource("providerById.json")),
                new TypeReference<JsonNode>() {
                });
        var token = string();
        var session = "{\"accessToken\": \"eyJhbGc\"}";
        String providerId = "12345";

        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller(
                "consent-manager-service", true)));
        providerServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(session));
        providerServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(providers.toString()));

        webTestClient.get()
                .uri("/providers/" + providerId)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.identifier.name").isEqualTo("Max Health Care")
                .jsonPath("$.identifier.id").isEqualTo(providerId)
                .jsonPath("$.city").isEqualTo("Bangalore")
                .jsonPath("$.telephone").isEqualTo("08080887876")
                .jsonPath("$.type").isEqualTo("prov");
    }

    public static class ContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertyValues values = TestPropertyValues.of(
                    Stream.of("consentmanager.clientregistry.url=" + providerServer.url("")));
            values.applyTo(applicationContext);
        }
    }

    @Test
    public void shouldGetGatewayTimeoutForDiscoverCareContext() throws Exception {
        var token = string();
        String requestId = "cecd3ed2-a7ea-406e-90f2-b51aa78741b9";
        String patientDiscoveryRequest = "{\n" +
                "  \"requestId\": \""+ requestId + "\",\n" +
                "  \"hip\": {\n" +
                "    \"id\": \"12345\"\n" +
                "  }\n" +
                "}";
        String userId = "test-user-id";
        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller(userId, false)));
        when(userServiceClient.userOf(userId)).thenReturn(Mono.just(TestBuilders.user().build()));
        when(discoveryRepository.getIfPresent(any())).thenReturn(Mono.empty());
        when(discoveryRepository.insert(anyString(), anyString(), any(), any())).thenReturn(Mono.empty());
        when(discoveryServiceClient.requestPatientFor(any(), eq("http://tmc.gov.in/ncg-gateway"), eq("12345"))).thenReturn(Mono.just(PatientResponse.builder().build()));
        webTestClient.post()
                .uri("/patients/care-contexts/discover")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, token)
                .bodyValue(patientDiscoveryRequest)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    public void shouldDiscoverCareContext() throws Exception {
        var token = string();
        String requestId = "cecd3ed2-a7ea-406e-90f2-b51aa78741b9";
        String patientDiscoveryRequest = "{\n" +
                "  \"requestId\": \""+ requestId + "\",\n" +
                "  \"hip\": {\n" +
                "    \"id\": \"12345\"\n" +
                "  }\n" +
                "}";
        String patientResponse = "{\n" +
                "  \"patient\": {\n" +
                "    \"referenceNumber\": \"string\",\n" +
                "    \"display\": \"string\",\n" +
                "    \"careContexts\": [\n" +
                "      {\n" +
                "        \"referenceNumber\": \"string\",\n" +
                "        \"display\": \"string\"\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";
        String userId = "test-user-id";
        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller(userId, false)));
        when(userServiceClient.userOf(userId)).thenReturn(Mono.just(TestBuilders.user().build()));
        when(discoveryRepository.getIfPresent(any())).thenReturn(Mono.empty());
        when(discoveryRepository.insert(anyString(), anyString(), any(), any())).thenReturn(Mono.empty());
        when(discoveryServiceClient.requestPatientFor(any(), eq("http://tmc.gov.in/ncg-gateway"), eq("12345"))).thenReturn(Mono.just(TestBuilders.patientResponse().build()));
        when(discoveryResults.get(any())).thenReturn(Mono.just(patientResponse));
        webTestClient.post()
                .uri("/patients/care-contexts/discover")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, token)
                .bodyValue(patientDiscoveryRequest)
                .exchange()
                .expectStatus().isOk();
    }
}