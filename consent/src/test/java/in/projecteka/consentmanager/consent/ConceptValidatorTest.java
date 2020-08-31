package in.projecteka.consentmanager.consent;

import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.consent.model.HIType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

import java.util.Arrays;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("dev")
public class ConceptValidatorTest {
    @Autowired
    private ConceptValidator validator;

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

    @SuppressWarnings("unused")
    @MockBean
    private ConsentRequestNotificationListener consentRequestNotificationListener;

    @SuppressWarnings("unused")
    @MockBean(name = "gatewayJWKSet")
    private JWKSet centralRegistryJWKSet;

    @SuppressWarnings("unused")
    @MockBean(name = "identityServiceJWKSet")
    private JWKSet identityServiceJWKSet;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void validatePurpose() {
        StepVerifier
                .create(validator.validatePurpose("BTG"))
                .expectNext(true)
                .expectComplete()
                .verify();
        StepVerifier
                .create(validator.validatePurpose("NONEXISTENT-PURPOSE"))
                .expectNext(false)
                .expectComplete()
                .verify();
    }

    @Test
    public void validateHiType() {
        StepVerifier
                .create(validator.validateHITypes(Arrays.asList(HIType.CONDITION.getValue(), "DiagnosticReport", "Observation", "MedicationRequest")))
                .expectNext(true)
                .expectComplete()
                .verify();
        StepVerifier
                .create(validator.validateHITypes(Arrays.asList("NONEXISTENT-HITYPE")))
                .expectNext(false)
                .expectComplete()
                .verify();

    }

}