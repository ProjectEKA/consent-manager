package in.projecteka.consentmanager;

import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.consentmanager.clients.ConsentArtefactNotifier;
import in.projecteka.consentmanager.common.GatewayTokenVerifier;
import in.projecteka.consentmanager.common.RequestValidator;
import in.projecteka.consentmanager.common.ServiceCaller;
import in.projecteka.consentmanager.consent.ConsentArtefactsController;
import in.projecteka.consentmanager.consent.ConsentNotificationPublisher;
import in.projecteka.consentmanager.consent.ConsentRequestNotificationListener;
import in.projecteka.consentmanager.consent.HipConsentNotificationListener;
import in.projecteka.consentmanager.consent.HiuConsentNotificationListener;
import in.projecteka.consentmanager.consent.PinVerificationTokenService;
import in.projecteka.consentmanager.dataflow.DataFlowBroadcastListener;
import in.projecteka.consentmanager.dataflow.DataFlowRequester;
import in.projecteka.consentmanager.dataflow.model.HealthInfoNotificationRequest;
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
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static in.projecteka.consentmanager.common.Role.GATEWAY;
import static in.projecteka.consentmanager.common.TestBuilders.string;
import static in.projecteka.consentmanager.dataflow.Constants.PATH_HEALTH_INFORMATION_NOTIFY;
import static in.projecteka.consentmanager.user.Constants.PATH_FIND_PATIENT;
import static in.projecteka.consentmanager.user.TestBuilders.patientRequest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
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
    private DataFlowBroadcastListener dataFlowBroadcastListener;

    @MockBean
    private ConsentRequestNotificationListener consentRequestNotificationListener;

    @MockBean
    private PinVerificationTokenService pinVerificationTokenService;

    @MockBean
    private RequestValidator validator;

    @MockBean
    private DataFlowRequester dataFlowRequester;

    @Autowired
    WebTestClient webTestClient;

    @Test
    void return401UnAuthorized() {
        webTestClient
                .post()
                .uri(PATH_FIND_PATIENT)
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
                .uri(PATH_FIND_PATIENT)
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
        var patientRequest = patientRequest().build();
        when(validator.validate(anyString(), anyString())).thenReturn(Mono.just(Boolean.TRUE));
        when(validator.put(anyString(), anyString())).thenReturn(Mono.empty());
        when(gatewayTokenVerifier.verify(token)).thenReturn(Mono.just(caller));

        webTestClient
                .post()
                .uri(PATH_FIND_PATIENT)
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .bodyValue(patientRequest)
                .exchange()
                .expectStatus()
                .isAccepted();
    }

    @Test
    void return202AcceptedForHealthInfoNotify() {
        var token = string();
        var username = string();
        var healthInfoNotification = HealthInfoNotificationRequest.builder()
                .requestId(UUID.randomUUID())
                .timestamp(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(2))
                .build();
        var caller = ServiceCaller.builder().clientId(username).roles(List.of(GATEWAY)).build();
        when(validator.put(anyString(), anyString())).thenReturn(Mono.empty());
        when(validator.validate(anyString(), anyString())).thenReturn(Mono.just(Boolean.TRUE));
        when(gatewayTokenVerifier.verify(token)).thenReturn(Mono.just(caller));
        when(dataFlowRequester.notifyHealthInformationStatus(any())).thenReturn(Mono.empty());

        webTestClient
                .post()
                .uri(PATH_HEALTH_INFORMATION_NOTIFY)
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .bodyValue(healthInfoNotification)
                .exchange()
                .expectStatus()
                .isAccepted();
    }
}