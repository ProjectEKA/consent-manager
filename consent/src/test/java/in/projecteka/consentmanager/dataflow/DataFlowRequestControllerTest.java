package in.projecteka.consentmanager.dataflow;

import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.consent.ConceptValidator;
import in.projecteka.consentmanager.consent.ConsentRequestNotificationListener;
import in.projecteka.consentmanager.consent.HipConsentNotificationListener;
import in.projecteka.consentmanager.consent.HiuConsentNotificationListener;
import in.projecteka.library.common.GatewayTokenVerifier;
import in.projecteka.library.common.RequestValidator;
import in.projecteka.library.common.ServiceCaller;
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

import static in.projecteka.consentmanager.common.TestBuilders.gatewayDataFlowRequest;
import static in.projecteka.consentmanager.common.TestBuilders.string;
import static in.projecteka.consentmanager.dataflow.Constants.PATH_HEALTH_INFORMATION_NOTIFY;
import static in.projecteka.consentmanager.dataflow.TestBuilders.healthInformationNotificationRequest;
import static in.projecteka.consentmanager.dataflow.TestBuilders.healthInformationResponseBuilder;
import static in.projecteka.library.common.Role.GATEWAY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
    @MockBean(name = "gatewayJWKSet")
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
    private RequestValidator validator;

    @MockBean
    DataFlowRequestRepository dataFlowRequestRepository;

    @Autowired
    private WebTestClient webClient;

    @Test
    void shouldReturnAcceptedForDataFlowRequest() {
        var token = string();
        var dataFlowRequestBody = gatewayDataFlowRequest().build();
        var caller = ServiceCaller.builder().clientId("Client_ID").roles(List.of(GATEWAY)).build();
        when(validator.put(anyString(), any())).thenReturn(Mono.empty());
        when(validator.validate(anyString(), any())).thenReturn(Mono.just(Boolean.TRUE));
        when(gatewayTokenVerifier.verify(token)).thenReturn(just(caller));
        when(dataFlowRequester.requestHealthDataInfo(any())).thenReturn(Mono.empty());

        webClient.post()
                .uri(in.projecteka.consentmanager.dataflow.Constants.PATH_HEALTH_INFORMATION_REQUEST)
                .accept(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dataFlowRequestBody))
                .exchange()
                .expectStatus()
                .isAccepted();
    }

    @Test
    void shouldFailWithTooManyRequestsErrorForInvalidDataFlowRequest() {
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
                .is4xxClientError();
    }

    @Test
    void shouldReturnAcceptedForHealthInfoNotification() {
        var token = string();
        var healthInformationNotificationRequest = healthInformationNotificationRequest().build();
        var caller = ServiceCaller.builder().clientId("Client_ID").roles(List.of(GATEWAY)).build();
        when(validator.put(anyString(), any())).thenReturn(Mono.empty());
        when(validator.validate(anyString(), any())).thenReturn(Mono.just(Boolean.TRUE));
        when(gatewayTokenVerifier.verify(token)).thenReturn(just(caller));
        when(dataFlowRequester.notifyHealthInformationStatus(healthInformationNotificationRequest))
                .thenReturn(Mono.empty());

        webClient.post()
                .uri(PATH_HEALTH_INFORMATION_NOTIFY)
                .accept(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(healthInformationNotificationRequest))
                .exchange()
                .expectStatus()
                .isAccepted();
    }

    @Test
    void shouldFailWithTooManyRequestsErrorForInvalidHealthInfoNotification() {
        var token = string();
        var healthInformationNotificationRequest = healthInformationNotificationRequest().build();
        var caller = ServiceCaller.builder().clientId("Client_ID").roles(List.of(GATEWAY)).build();
        when(validator.validate(anyString(), any())).thenReturn(Mono.just(Boolean.FALSE));
        when(gatewayTokenVerifier.verify(token)).thenReturn(just(caller));
        when(dataFlowRequester.notifyHealthInformationStatus(healthInformationNotificationRequest))
                .thenReturn(Mono.empty());

        webClient.post()
                .uri(PATH_HEALTH_INFORMATION_NOTIFY)
                .accept(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(healthInformationNotificationRequest))
                .exchange()
                .expectStatus()
                .is4xxClientError();
    }

    @Test
    void shouldReturnAcceptedForDataFlowResponse() {
        var token = string();
        var healthInformationResponse = healthInformationResponseBuilder().build();
        var caller = ServiceCaller.builder().clientId("Client_ID").roles(List.of(GATEWAY)).build();
        when(validator.put(anyString(), any())).thenReturn(Mono.empty());
        when(validator.validate(anyString(), any())).thenReturn(Mono.just(Boolean.TRUE));
        when(gatewayTokenVerifier.verify(token)).thenReturn(just(caller));
        when(dataFlowRequester.updateDataflowRequestStatus(healthInformationResponse)).thenReturn(Mono.empty());

        webClient.post()
                .uri(in.projecteka.consentmanager.dataflow.Constants.PATH_HEALTH_INFORMATION_ON_REQUEST)
                .header(AUTHORIZATION,token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(healthInformationResponse))
                .exchange()
                .expectStatus()
                .isAccepted();
    }

    @Test
    void shouldFailWithTooManyRequestsErrorForInvalidDataFlowResponse() {
        var token = string();
        var healthInformationResponse = healthInformationResponseBuilder().build();
        var caller = ServiceCaller.builder().clientId("Client_ID").roles(List.of(GATEWAY)).build();
        when(validator.validate(anyString(), any())).thenReturn(Mono.just(Boolean.FALSE));
        when(gatewayTokenVerifier.verify(token)).thenReturn(just(caller));
        when(dataFlowRequester.updateDataflowRequestStatus(healthInformationResponse)).thenReturn(Mono.empty());

        webClient.post()
                .uri("/v1/health-information/on-request")
                .header(AUTHORIZATION,token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(healthInformationResponse))
                .exchange()
                .expectStatus()
                .is4xxClientError();
    }
}
