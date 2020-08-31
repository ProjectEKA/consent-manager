package in.projecteka.consentmanager;

import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.consentmanager.clients.ConsentArtefactNotifier;
import in.projecteka.consentmanager.consent.ConsentArtefactsController;
import in.projecteka.consentmanager.consent.ConsentManager;
import in.projecteka.consentmanager.consent.ConsentNotificationPublisher;
import in.projecteka.consentmanager.consent.ConsentRequestNotificationListener;
import in.projecteka.consentmanager.consent.HipConsentNotificationListener;
import in.projecteka.consentmanager.consent.HiuConsentNotificationListener;
import in.projecteka.consentmanager.consent.PinVerificationTokenService;
import in.projecteka.library.common.GatewayTokenVerifier;
import in.projecteka.library.common.RequestValidator;
import in.projecteka.library.common.ServiceCaller;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static in.projecteka.consentmanager.common.TestBuilders.string;
import static in.projecteka.consentmanager.consent.Constants.PATH_CONSENTS_FETCH;
import static in.projecteka.consentmanager.consent.Constants.PATH_CONSENT_REQUESTS_INIT;
import static in.projecteka.consentmanager.consent.TestBuilders.fetchRequest;
import static in.projecteka.library.common.Role.GATEWAY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static reactor.core.publisher.Mono.empty;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "60000")
class SecurityConfigurationTest {
    @MockBean(name = "gatewayJWKSet")
    JWKSet centralRegistryJWKSet;

    @MockBean(name = "identityServiceJWKSet")
    JWKSet identityServiceJWKSet;

    @MockBean
    GatewayTokenVerifier gatewayTokenVerifier;

    @MockBean
    private DestinationsConfig destinationsConfig;

    @MockBean
    private ConsentArtefactsController consentArtefactsController;

    @MockBean
    private ConsentNotificationPublisher postConsentApproval;

    @MockBean
    private HipConsentNotificationListener hipConsentNotificationListener;

    @MockBean
    private HiuConsentNotificationListener hiuConsentNotificationListener;

    @MockBean
    private ConsentArtefactNotifier consentArtefactNotifier;

    @MockBean
    private ConsentRequestNotificationListener consentRequestNotificationListener;

    @MockBean
    private PinVerificationTokenService pinVerificationTokenService;

    @MockBean
    private RequestValidator validator;

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    ConsentManager consentManager;

    @Test
    void return401UnAuthorized() {
        webTestClient
                .post()
                .uri(PATH_CONSENT_REQUESTS_INIT)
                .contentType(APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isEqualTo(UNAUTHORIZED);
    }

    @Test
    void returnForbiddenError() {
        var token = string();
        var caller = ServiceCaller.builder().roles(List.of()).build();
        when(gatewayTokenVerifier.verify(token)).thenReturn(Mono.just(caller));

        webTestClient
                .post()
                .uri(PATH_CONSENT_REQUESTS_INIT)
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    void return202Accepted() {
        var token = string();
        var caller = ServiceCaller.builder().roles(List.of(GATEWAY)).build();
        when(validator.validate(anyString(), any(LocalDateTime.class))).thenReturn(Mono.just(Boolean.TRUE));
        when(validator.put(anyString(), any(LocalDateTime.class))).thenReturn(empty());
        when(gatewayTokenVerifier.verify(token)).thenReturn(Mono.just(caller));
        when(consentManager.getConsent(any(), any(UUID.class))).thenReturn(empty());

        webTestClient
                .post()
                .uri(PATH_CONSENTS_FETCH)
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .bodyValue(fetchRequest().build())
                .exchange()
                .expectStatus()
                .isAccepted();
    }
}