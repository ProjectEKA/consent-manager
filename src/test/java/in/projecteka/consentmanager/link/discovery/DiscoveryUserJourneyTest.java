package in.projecteka.consentmanager.link.discovery;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.clients.DiscoveryServiceClient;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.clients.model.Error;
import in.projecteka.consentmanager.clients.model.ErrorCode;
import in.projecteka.consentmanager.clients.model.ErrorRepresentation;
import in.projecteka.consentmanager.common.Authenticator;
import in.projecteka.consentmanager.common.Caller;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import in.projecteka.consentmanager.consent.ConceptValidator;
import in.projecteka.consentmanager.consent.ConsentRequestNotificationListener;
import in.projecteka.consentmanager.consent.HipConsentNotificationListener;
import in.projecteka.consentmanager.consent.HiuConsentNotificationListener;
import in.projecteka.consentmanager.dataflow.DataFlowBroadcastListener;
import in.projecteka.consentmanager.link.discovery.model.patient.response.DiscoveryResponse;
import in.projecteka.consentmanager.link.discovery.model.patient.response.DiscoveryResult;
import in.projecteka.consentmanager.link.discovery.model.patient.response.GatewayResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.hamcrest.Matchers;
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

import static in.projecteka.consentmanager.consent.TestBuilders.OBJECT_MAPPER;
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

    @SuppressWarnings("unused")
    @MockBean
    private ConceptValidator conceptValidator;

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
        when(discoveryServiceClient.requestPatientFor(any(), eq("12345"))).thenReturn(Mono.just(true));
        when(discoveryResults.get(any())).thenReturn(Mono.empty());
        var errorResponse = new ErrorRepresentation(
                new Error(ErrorCode.NO_RESULT_FROM_GATEWAY,"Didn't receive any result from Gateway"));
        var errorResponseJson = OBJECT_MAPPER.writeValueAsString(errorResponse);
        webTestClient.post()
                .uri("/care-contexts/discover")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, token)
                .bodyValue(patientDiscoveryRequest)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .json(errorResponseJson);
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
                "    \"referenceNumber\": \"XYZPatientUuid\",\n" +
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
        when(discoveryServiceClient.requestPatientFor(any(), eq("12345"))).thenReturn(Mono.just(true));
        when(discoveryResults.get(any())).thenReturn(Mono.just(patientResponse));
        webTestClient.post()
                .uri("/care-contexts/discover")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, token)
                .bodyValue(patientDiscoveryRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(DiscoveryResponse.class)
                .value(DiscoveryResponse::getPatient, Matchers.notNullValue());
    }

    @Test
    public void shouldFailDiscoverCareContext() throws Exception {
        var token = string();
        String requestId = "cecd3ed2-a7ea-406e-90f2-b51aa78741b9";
        String patientDiscoveryRequest = "{\n" +
                "  \"requestId\": \""+ requestId + "\",\n" +
                "  \"hip\": {\n" +
                "    \"id\": \"12345\"\n" +
                "  }\n" +
                "}";
        String patientResponse = "{\n" +
                "  \"requestId\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\n" +
                "  \"transactionId\": \"2b7778a0-9eb7-4ed4-8693-ed8be2eac9d2\",\n" +
                "  \"patient\": null,\n" +
                "  \"error\": {\n" +
                "    \"code\": 1000,\n" +
                "    \"message\": \"Could not identify a unique patient. Need more information\"\n" +
                "  }\n" +
                "}";
        String userId = "test-user-id";
        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller(userId, false)));
        when(userServiceClient.userOf(userId)).thenReturn(Mono.just(TestBuilders.user().build()));
        when(discoveryRepository.getIfPresent(any())).thenReturn(Mono.empty());
        when(discoveryRepository.insert(anyString(), anyString(), any(), any())).thenReturn(Mono.empty());
        when(discoveryServiceClient.requestPatientFor(any(), eq("12345"))).thenReturn(Mono.just(true));
        when(discoveryResults.get(any())).thenReturn(Mono.just(patientResponse));
        var errorResponse = new ErrorRepresentation(
                new Error(ErrorCode.NO_PATIENT_FOUND,"Could not find patient information"));
        var errorResponseJson = OBJECT_MAPPER.writeValueAsString(errorResponse);
        webTestClient.post()
                .uri("/care-contexts/discover")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, token)
                .bodyValue(patientDiscoveryRequest)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .json(errorResponseJson);
    }

    @Test
    public void shouldFailDiscoverCareContextForZeroPatientResponse() throws Exception {
        var token = string();
        String requestId = "cecd3ed2-a7ea-406e-90f2-b51aa78741b9";
        String patientDiscoveryRequest = "{\n" +
                "  \"requestId\": \""+ requestId + "\",\n" +
                "  \"hip\": {\n" +
                "    \"id\": \"12345\"\n" +
                "  }\n" +
                "}";
        String patientResponse = "{\n" +
                "  \"requestId\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\n" +
                "  \"transactionId\": \"2b7778a0-9eb7-4ed4-8693-ed8be2eac9d2\",\n" +
                "  \"patient\": null\n" +
                "}";
        String userId = "test-user-id";
        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller(userId, false)));
        when(userServiceClient.userOf(userId)).thenReturn(Mono.just(TestBuilders.user().build()));
        when(discoveryRepository.getIfPresent(any())).thenReturn(Mono.empty());
        when(discoveryRepository.insert(anyString(), anyString(), any(), any())).thenReturn(Mono.empty());
        when(discoveryServiceClient.requestPatientFor(any(), eq("12345"))).thenReturn(Mono.just(true));
        when(discoveryResults.get(any())).thenReturn(Mono.just(patientResponse));
        var errorResponse = new ErrorRepresentation(
                new Error(ErrorCode.NO_PATIENT_FOUND,"Could not find patient information"));
        var errorResponseJson = OBJECT_MAPPER.writeValueAsString(errorResponse);
        webTestClient.post()
                .uri("/care-contexts/discover")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, token)
                .bodyValue(patientDiscoveryRequest)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .json(errorResponseJson);
    }

    @Test
    public void onDiscoverPatientCareContexts() {
        var token = string();
        var patientDiscoveryResult = TestBuilders.discoveryResult().build();
        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller("test-user-id", false)));
        webTestClient.post()
                .uri("/care-contexts/on-discover")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, token)
                .bodyValue(patientDiscoveryResult)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void shouldFailWhenRequestIdIsNotGiven() throws Exception {
        var token = string();
        var gatewayResponse = GatewayResponse.builder()
                .requestId(null)
                .build();
        var error = Error.builder()
                .code(ErrorCode.NO_PATIENT_FOUND)
                .message("Could not identify a unique patient. Need more information.")
                .build();
        var patientDiscoveryResult = DiscoveryResult.builder()
                .patient(null)
                .error(error)
                .resp(gatewayResponse)
                .build();
        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller("test-user-id", false)));
        webTestClient.post()
                .uri("/care-contexts/on-discover")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, token)
                .bodyValue(patientDiscoveryResult)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    public void shouldFailOnDiscoverPatientCareContexts() throws Exception {
        var token = string();
        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller("test-user-id", false)));
        webTestClient.post()
                .uri("/care-contexts/on-discover")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, token)
                .exchange()
                .expectStatus().is5xxServerError();
    }

}