package in.projecteka.dataflow;

import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.dataflow.model.ConsentArtefactRepresentation;
import in.projecteka.dataflow.model.hip.DataRequest;
import in.projecteka.library.common.GatewayTokenVerifier;
import in.projecteka.library.common.RequestValidator;
import in.projecteka.library.common.ServiceCaller;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.util.List;

import static in.projecteka.dataflow.Constants.PATH_HEALTH_INFORMATION_NOTIFY;
import static in.projecteka.dataflow.Constants.PATH_HEALTH_INFORMATION_ON_REQUEST;
import static in.projecteka.dataflow.Constants.PATH_HEALTH_INFORMATION_REQUEST;
import static in.projecteka.dataflow.TestBuilders.dataRequest;
import static in.projecteka.dataflow.TestBuilders.gatewayDataFlowRequest;
import static in.projecteka.dataflow.TestBuilders.healthInformationNotificationRequest;
import static in.projecteka.dataflow.TestBuilders.healthInformationResponseBuilder;
import static in.projecteka.dataflow.TestBuilders.string;
import static in.projecteka.library.common.Role.GATEWAY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.just;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class DataFlowRequestControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private PostDataFlowRequestApproval postDataFlowRequestApproval;

    @MockBean
    private DataFlowBroadcastListener dataFlowBroadcastListener;

    @SuppressWarnings("unused")
    @MockBean
    private DataRequestNotifier dataRequestNotifier;

    @SuppressWarnings("unused")
    @MockBean(name = "gatewayJWKSet")
    private JWKSet centralRegistryJWKSet;

    @SuppressWarnings("unused")
    @MockBean(name = "identityServiceJWKSet")
    private JWKSet identityServiceJWKSet;

    @MockBean
    private GatewayTokenVerifier gatewayTokenVerifier;

    @MockBean
    private RequestValidator validator;

    @MockBean
    private ConsentManagerClient consentManagerClient;

    @MockBean
    private DataFlowRequester dataFlowRequester;

    @Test
    void shouldSendDataRequestToHip() {
        DataRequest dataRequest = dataRequest().build();
        String hipId = string();
        String consentId = string();
        ConsentArtefactRepresentation caRep = ConsentArtefactRepresentation.builder().build();

        when(consentManagerClient.getConsentArtefact(consentId)).thenReturn(just(caRep));
        when(dataRequestNotifier.notifyHip(dataRequest, hipId)).thenReturn(empty());

        dataFlowBroadcastListener.configureAndSendDataRequestFor(dataRequest);

        verify(dataFlowBroadcastListener).configureAndSendDataRequestFor(dataRequest);
    }

    @Test
    void shouldReturnAcceptedForDataFlowRequest() {
        var token = string();
        var dataFlowRequestBody = gatewayDataFlowRequest().build();
        var caller = ServiceCaller.builder().clientId("Client_ID").roles(List.of(GATEWAY)).build();
        when(validator.put(anyString(), any())).thenReturn(Mono.empty());
        when(validator.validate(anyString(), any())).thenReturn(Mono.just(Boolean.TRUE));
        when(gatewayTokenVerifier.verify(token)).thenReturn(just(caller));
        when(dataFlowRequester.requestHealthDataInfo(any())).thenReturn(Mono.empty());

        webTestClient
                .post()
                .uri(PATH_HEALTH_INFORMATION_REQUEST)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, token)
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
        when(validator.validate(anyString(), any())).thenReturn(Mono.just(Boolean.FALSE));
        when(gatewayTokenVerifier.verify(token)).thenReturn(just(caller));
        when(dataFlowRequester.requestHealthDataInfo(any())).thenReturn(Mono.empty());

        webTestClient
                .post()
                .uri(PATH_HEALTH_INFORMATION_REQUEST)
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

        webTestClient
                .post()
                .uri(PATH_HEALTH_INFORMATION_NOTIFY)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, token)
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

        webTestClient
                .post()
                .uri(PATH_HEALTH_INFORMATION_NOTIFY)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, token)
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

        webTestClient
                .post()
                .uri(PATH_HEALTH_INFORMATION_ON_REQUEST)
                .header(HttpHeaders.AUTHORIZATION, token)
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

        webTestClient
                .post()
                .uri(PATH_HEALTH_INFORMATION_ON_REQUEST)
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(healthInformationResponse))
                .exchange()
                .expectStatus()
                .is4xxClientError();
    }
}

