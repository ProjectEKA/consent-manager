package in.projecteka.consentmanager.link.discovery;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.consent.ConsentRequestNotificationListener;
import in.projecteka.consentmanager.consent.HipConsentNotificationListener;
import in.projecteka.consentmanager.consent.HiuConsentNotificationListener;
import in.projecteka.consentmanager.dataflow.DataFlowBroadcastListener;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static in.projecteka.consentmanager.link.discovery.TestBuilders.string;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
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
    private static MockWebServer providerServer = new MockWebServer();
    private static MockWebServer identityServer = new MockWebServer();

    @Autowired
    private WebTestClient webTestClient;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @AfterAll
    public static void tearDown() throws IOException {
        providerServer.shutdown();
        identityServer.shutdown();
    }

    @Test
    public void shouldGetProvidersByName() throws IOException {
        var providers = new ObjectMapper().readValue(
                Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResource("provider.json")),
                new TypeReference<List<JsonNode>>() {
                });
        var token = string();
        var user = "{\"preferred_username\": \"service-account-consent-manager-service\"}";
        var session = "{\"accessToken\": \"eyJhbGc\"}";
        identityServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody(user));
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

    public static class ContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertyValues values = TestPropertyValues.of(
                    Stream.of("consentmanager.clientregistry.url=" + providerServer.url(""),
                            "consentmanager.keycloak.baseUrl=" + identityServer.url("")));
            values.applyTo(applicationContext);
        }
    }
}