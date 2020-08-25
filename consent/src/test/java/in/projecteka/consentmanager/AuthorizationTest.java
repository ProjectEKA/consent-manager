package in.projecteka.consentmanager;

import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.consentmanager.consent.ConceptValidator;
import in.projecteka.consentmanager.consent.ConsentManager;
import in.projecteka.consentmanager.consent.ConsentRequestNotificationListener;
import in.projecteka.consentmanager.consent.HipConsentNotificationListener;
import in.projecteka.consentmanager.consent.HiuConsentNotificationListener;
import in.projecteka.consentmanager.consent.ConsentNotificationPublisher;
import in.projecteka.consentmanager.dataflow.DataFlowBroadcastListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

@SuppressWarnings("ALL")

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class AuthorizationTest {
    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ConsentManager consentManager;

    @MockBean
    private ConsentNotificationPublisher consentNotificationPublisher;

    @MockBean
    private DestinationsConfig destinationsConfig;

    @MockBean
    private HiuConsentNotificationListener hiuConsentNotificationListener;

    @MockBean
    private HipConsentNotificationListener hipConsentNotificationListener;

    @MockBean
    private DataFlowBroadcastListener dataFlowBroadcastListener;

    @SuppressWarnings("unused")
    @MockBean
    private ConsentRequestNotificationListener consentRequestNotificationListener;

    @SuppressWarnings("unused")
    @MockBean(name = "gatewayJWKSet")
    private JWKSet centralRegistryJWKSet;

    @SuppressWarnings("unused")
    @MockBean(name = "identityServiceJWKSet")
    private JWKSet identityServiceJWKSet;

    @SuppressWarnings("unused")
    @MockBean
    private ConceptValidator conceptValidator;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @ParameterizedTest(name = "Authentication Test")
    @CsvSource({
            ",",
            "empty",
            "null"
    })
    public void notAuthenticatedToUse(@ConvertWith(NullableConverter.class) String header) {
        webTestClient.get()
                .uri("/providers?name=Max")
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, header)
                .exchange()
                .expectStatus().is4xxClientError();
    }
}

