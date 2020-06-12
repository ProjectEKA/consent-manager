package in.projecteka.consentmanager.dataflow;

import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.common.Caller;
import in.projecteka.consentmanager.common.CentralRegistryTokenVerifier;
import in.projecteka.consentmanager.consent.ConceptValidator;
import in.projecteka.consentmanager.consent.ConsentRequestNotificationListener;
import in.projecteka.consentmanager.consent.HipConsentNotificationListener;
import in.projecteka.consentmanager.consent.HiuConsentNotificationListener;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import static in.projecteka.consentmanager.dataflow.TestBuilders.gatewayDataFlowRequest;
import static in.projecteka.consentmanager.user.TestBuilders.string;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static reactor.core.publisher.Mono.just;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureWebTestClient
class DataFlowRequestControllerTest {

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

    @Autowired
    private WebTestClient webClient;

    @SuppressWarnings("unused")
    @MockBean(name = "centralRegistryJWKSet")
    private JWKSet centralRegistryJWKSet;

    @SuppressWarnings("unused")
    @MockBean(name = "identityServiceJWKSet")
    private JWKSet identityServiceJWKSet;

    @MockBean
    private CentralRegistryTokenVerifier centralRegistryTokenVerifier;

    @SuppressWarnings("unused")
    @MockBean
    private ConceptValidator conceptValidator;

    @Test
    void shouldReturnAcceptedForDataFlowRequest() {
        var username = string();
        var token = string();
        var dataFlowRequestBody = gatewayDataFlowRequest().build();
        when(centralRegistryTokenVerifier.verify(token)).thenReturn(just(new Caller(username, true)));
        webClient.post()
                .uri("/v1/health-information/request")
                .accept(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dataFlowRequestBody))
                .exchange()
                .expectStatus()
                .isAccepted();


    }
}
