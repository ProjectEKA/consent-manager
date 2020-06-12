package in.projecteka.consentmanager;

import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.consentmanager.clients.ConsentArtefactNotifier;
import in.projecteka.consentmanager.common.CentralRegistryTokenVerifier;
import in.projecteka.consentmanager.common.CentralRegistryTokenVerifierForGateway;
import in.projecteka.consentmanager.consent.ConsentArtefactsController;
import in.projecteka.consentmanager.consent.ConsentManager;
import in.projecteka.consentmanager.consent.ConsentNotificationPublisher;
import in.projecteka.consentmanager.consent.ConsentRequestNotificationListener;
import in.projecteka.consentmanager.consent.HipConsentNotificationListener;
import in.projecteka.consentmanager.consent.HiuConsentNotificationListener;
import in.projecteka.consentmanager.consent.PinVerificationTokenService;
import in.projecteka.consentmanager.dataflow.DataFlowBroadcastListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;


import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "60000")
class SecurityConfigurationTest {
    @MockBean(name = "centralRegistryJWKSet")
    JWKSet centralRegistryJWKSet;

    @MockBean(name = "identityServiceJWKSet")
    JWKSet identityServiceJWKSet;

    @MockBean
    CentralRegistryTokenVerifierForGateway centralRegistryTokenVerifierForGateway;

    @MockBean
    CentralRegistryTokenVerifier centralRegistryTokenVerifier;

    @MockBean
    private DestinationsConfig destinationsConfig;

    @MockBean
    private ConsentArtefactsController consentArtefactsController;

    @MockBean
    private ConsentNotificationPublisher postConsentApproval;

    @MockBean
    private ConsentManager consentRequestService;

    @MockBean
    private HipConsentNotificationListener hipConsentNotificationListener;

    @MockBean
    private HiuConsentNotificationListener hiuConsentNotificationListener;

    @MockBean
    private ConsentArtefactNotifier consentArtefactNotifier;

    @MockBean
    private DataFlowBroadcastListener dataFlowBroadcastListener;

    @MockBean
    private ConsentRequestNotificationListener consentRequestNotificationListener;

    @MockBean
    private PinVerificationTokenService pinVerificationTokenService;

    @Autowired
    WebTestClient webTestClient;

    @Test
    void return401UnAuthorized() {
        webTestClient
                .post()
                .uri("/v1/patients/find")
                .contentType(APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isEqualTo(UNAUTHORIZED);
    }

}