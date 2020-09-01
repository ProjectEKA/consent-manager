package in.projecteka.consentmanager.consent;

import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.consentmanager.consent.model.response.HIPCosentNotificationAcknowledgment;
import in.projecteka.library.common.Authenticator;
import in.projecteka.library.common.GatewayTokenVerifier;
import in.projecteka.library.common.RequestValidator;
import in.projecteka.library.common.ServiceCaller;
import in.projecteka.library.common.cache.CacheAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static in.projecteka.consentmanager.consent.Constants.PATH_HIP_CONSENT_ON_NOTIFY;
import static in.projecteka.consentmanager.consent.TestBuilders.string;
import static in.projecteka.library.common.Role.GATEWAY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static reactor.core.publisher.Mono.just;

class ConsentArtefactsControllerTest {

    @MockBean
    private  ConsentManager consentManager;

    @MockBean
    private CacheAdapter<String, String> cache;

    @MockBean
    private  ConsentServiceProperties serviceProperties;

    @MockBean
    private  RequestValidator validator;

    @Autowired
    private WebTestClient webTestClient;

    @SuppressWarnings("unused")
    @MockBean(name = "gatewayJWKSet")
    private JWKSet centralRegistryJWKSet;

    @SuppressWarnings("unused")
    @MockBean(name = "identityServiceJWKSet")
    private JWKSet identityServiceJWKSet;

    @MockBean
    private GatewayTokenVerifier gatewayTokenVerifier;

    @MockBean
    private Authenticator authenticator;

    @Test
    void shouldUpdateConsentNotificationInCaseOfRevoke() {
        var token = string();
        var request = TestBuilders.hipConsentNotificationAcknowledgement().build();
        var caller = ServiceCaller.builder().clientId("Client_ID").roles(List.of(GATEWAY)).build();
        when(validator.put(anyString(), any(LocalDateTime.class))).thenReturn(Mono.empty());
        when(validator.validate(anyString(), any(LocalDateTime.class))).thenReturn(Mono.just(Boolean.TRUE));
        when(gatewayTokenVerifier.verify(token)).thenReturn(just(caller));
        when(cache.exists(request.getAcknowledgement().getConsentId())).thenReturn(Mono.just(false));
        when(validator.validate(request.getRequestId().toString(), request.getTimestamp())).thenReturn(Mono.just(true));
        when(consentManager.updateConsentNotification(request)).thenReturn(Mono.empty());
        when(validator.put(anyString(), any(LocalDateTime.class))).thenReturn(Mono.empty());

        webTestClient
                .post()
                .uri(PATH_HIP_CONSENT_ON_NOTIFY)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus()
                .isAccepted();
    }

}