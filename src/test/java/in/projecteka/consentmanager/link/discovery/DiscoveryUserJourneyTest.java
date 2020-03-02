package in.projecteka.consentmanager.link.discovery;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.consent.ConsentArtefactBroadcastListener;
import in.projecteka.consentmanager.consent.ConsentRequestNotificationListener;
import in.projecteka.consentmanager.dataflow.DataFlowBroadcastListener;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
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

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ContextConfiguration(initializers = DiscoveryUserJourneyTest.ContextInitializer.class)
public class DiscoveryUserJourneyTest {

    private static MockWebServer mockWebServer = new MockWebServer();

    @MockBean
    private DestinationsConfig destinationsConfig;

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ConsentArtefactBroadcastListener consentArtefactBroadcastListener;

    @MockBean
    private DataFlowBroadcastListener dataFlowBroadcastListener;

    @MockBean
    private ConsentRequestNotificationListener consentRequestNotificationListener;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldGetProvidersByName() throws IOException {
        var jsonNode = new ObjectMapper().readValue(
                Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResource("provider.json")),
                new TypeReference<List<JsonNode>>() {
                });
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody(jsonNode.toString()));

        webTestClient.get()
                .uri("/providers?name=Max")
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "MTIzNDU2Nzg5")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.[0].identifier.name").isEqualTo("Max Health Care")
                .jsonPath("$.[0].identifier.id").isEqualTo("12345")
                .jsonPath("$.[0].city").isEqualTo("Bangalore")
                .jsonPath("$.[0].telephone").isEqualTo("08080887876")
                .jsonPath("$.[0].type").isEqualTo("prov");

        mockWebServer.shutdown();
    }

    public static class ContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertyValues values = TestPropertyValues.of(
                    Stream.of(
                            "consentmanager.clientregistry.url=" + mockWebServer.url("")
                    )
            );
            values.applyTo(applicationContext);
        }
    }
}