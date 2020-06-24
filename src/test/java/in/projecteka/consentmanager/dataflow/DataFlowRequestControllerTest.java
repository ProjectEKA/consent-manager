package in.projecteka.consentmanager.dataflow;

import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.common.GatewayTokenVerifier;
import in.projecteka.consentmanager.common.ServiceCaller;
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
import reactor.core.publisher.Mono;

import java.util.List;

import static in.projecteka.consentmanager.common.Constants.V_1_HEALTH_INFORMATION_NOTIFY;
import static in.projecteka.consentmanager.common.Role.GATEWAY;
import static in.projecteka.consentmanager.dataflow.TestBuilders.gatewayDataFlowRequest;
import static in.projecteka.consentmanager.dataflow.TestBuilders.healthInformationNotificationRequest;
import static in.projecteka.consentmanager.dataflow.TestBuilders.healthInformationResponseBuilder;
import static in.projecteka.consentmanager.user.TestBuilders.string;
import static org.mockito.ArgumentMatchers.any;
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

    @SuppressWarnings("unused")
    @MockBean(name = "centralRegistryJWKSet")
    private JWKSet centralRegistryJWKSet;

    @SuppressWarnings("unused")
    @MockBean(name = "identityServiceJWKSet")
    private JWKSet identityServiceJWKSet;

    @MockBean
    private GatewayTokenVerifier gatewayTokenVerifier;

    @MockBean
    private DataFlowRequester dataFlowRequester;

    @SuppressWarnings("unused")
    @MockBean
    private ConceptValidator conceptValidator;

    @MockBean
    DataFlowRequestRepository dataFlowRequestRepository;

    @Autowired
    private WebTestClient webClient;

    @Test
    void shouldReturnAcceptedForDataFlowRequest() {
        var token = string();
        var dataFlowRequestBody = gatewayDataFlowRequest().build();
        var caller = ServiceCaller.builder().clientId("Client_ID").roles(List.of(GATEWAY)).build();

        when(gatewayTokenVerifier.verify(token)).thenReturn(just(caller));
        when(dataFlowRequester.requestHealthDataInfo(any())).thenReturn(Mono.empty());

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

    @Test
    void shouldReturnAcceptedForHealthInfoNotification() {
        var token = string();
        var healthInformationNotificationRequest = healthInformationNotificationRequest().build();
        var caller = ServiceCaller.builder().clientId("Client_ID").roles(List.of(GATEWAY)).build();

        when(gatewayTokenVerifier.verify(token)).thenReturn(just(caller));
        when(dataFlowRequester.notifyHealthInformationStatus(healthInformationNotificationRequest))
                .thenReturn(Mono.empty());

        webClient.post()
                .uri(V_1_HEALTH_INFORMATION_NOTIFY)
                .accept(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(healthInformationNotificationRequest))
                .exchange()
                .expectStatus()
                .isAccepted();
    }

    @Test
    void shouldReturnAcceptedForDataFlowResponse() {
        var token = string();
        var healthInformationResponse = healthInformationResponseBuilder().build();
        var caller = ServiceCaller.builder().clientId("Client_ID").roles(List.of(GATEWAY)).build();
        when(gatewayTokenVerifier.verify(token)).thenReturn(just(caller));
        when(dataFlowRequestRepository.updateDataFlowRequestStatus(healthInformationResponse.getHiRequest().getTransactionId(),
                healthInformationResponse.getHiRequest().getSessionStatus())).thenReturn(Mono.empty());

        webClient.post()
                .uri("/v1/health-information/on-request")
                .header(AUTHORIZATION,token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(healthInformationResponse))
                .exchange()
                .expectStatus()
                .isAccepted();
    }
}
