package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.model.HealthAccountServiceTokenResponse;
import in.projecteka.consentmanager.clients.model.OtpAction;
import in.projecteka.consentmanager.clients.model.OtpCommunicationData;
import in.projecteka.consentmanager.clients.model.OtpGenerationDetail;
import in.projecteka.consentmanager.clients.model.OtpRequest;
import in.projecteka.consentmanager.clients.model.OtpRequestResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.consentmanager.clients.model.OtpCommunicationData;
import in.projecteka.consentmanager.clients.model.OtpRequest;
import in.projecteka.consentmanager.clients.model.OtpRequestResponse;
import in.projecteka.consentmanager.clients.model.HealthAccountServiceTokenResponse;
import in.projecteka.consentmanager.clients.model.StateRequestResponse;
import in.projecteka.consentmanager.clients.model.DistrictData;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

class HealthAccountServiceClientTest {
    private @Captor
    ArgumentCaptor<ClientRequest> captor;
    @Mock
    private ExchangeFunction exchangeFunction;
    private HealthAccountServiceClient healthAccountServiceClient;
    private EasyRandom easyRandom;

    @BeforeEach
    void setUp() {
        easyRandom = new EasyRandom();

        MockitoAnnotations.initMocks(this);
        WebClient.Builder webClientBuilder = WebClient.builder().exchangeFunction(exchangeFunction);
        healthAccountServiceClient = new HealthAccountServiceClient(webClientBuilder, "http://localhost/healthaccountservice");
    }

    @Test
    public void shouldSendOTPRequest() {
        var sessionId = easyRandom.nextObject(String.class);
        var value = easyRandom.nextObject(String.class);
        OtpGenerationDetail generationDetail = OtpGenerationDetail.builder().action(OtpAction.REGISTRATION.toString()).systemName("APP").build();
        var otpRequest = new OtpRequest(sessionId, new OtpCommunicationData("MOBILE", value), generationDetail);

        when(exchangeFunction.exchange(captor.capture())).thenReturn(
                Mono.just(
                        ClientResponse
                                .create(HttpStatus.OK)
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .body("{\"txnID\":\"12345\"}")
                                .build()));

        Mono<OtpRequestResponse> response = healthAccountServiceClient.send(otpRequest);

        StepVerifier.create(response).assertNext(
                otpRequestResponse -> assertThat(otpRequestResponse.getTransactionId()).isEqualTo("12345")
        ).verifyComplete();

        assertThat(captor.getValue().url().getPath()).isEqualTo("/healthaccountservice/v1/ha/generate_mobile_otp");
        assertThat(captor.getValue().url().getHost()).isEqualTo("localhost");
    }

    @Test
    public void shouldVerifyOTPForGivenTransactionId() {
        var sessionId = easyRandom.nextObject(String.class);
        var otpValue = easyRandom.nextObject(String.class);

        when(exchangeFunction.exchange(captor.capture())).thenReturn(
                Mono.just(
                        ClientResponse
                                .create(HttpStatus.OK)
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .body("{\"token\":\"12345\"}")
                                .build()));

        Mono<HealthAccountServiceTokenResponse> response = healthAccountServiceClient.verifyOtp(sessionId, otpValue);

        StepVerifier.create(response).assertNext(
                verificationTokenResponse
                        -> assertThat(verificationTokenResponse.getToken()).isEqualTo("12345")
        ).verifyComplete();

        assertThat(captor.getValue().url().getPath()).isEqualTo("/healthaccountservice/v1/ha/verify_mobile_otp");
        assertThat(captor.getValue().url().getHost()).isEqualTo("localhost");
    }

    @Test
    public void shouldGiveClientErrorWhenOTPIsInvalid() {
        var sessionId = easyRandom.nextObject(String.class);
        var otpValue = easyRandom.nextObject(String.class);

        when(exchangeFunction.exchange(captor.capture())).thenReturn(
                Mono.just(
                        ClientResponse
                                .create(HttpStatus.UNAUTHORIZED)
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .body("{\"error\":\"invalid\"}")
                                .build()));

        Mono<HealthAccountServiceTokenResponse> response = healthAccountServiceClient.verifyOtp(sessionId, otpValue);

        StepVerifier.create(healthAccountServiceClient.verifyOtp(sessionId, otpValue))
                .expectErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().value() == 401)
                .verify();

        assertThat(captor.getValue().url().getPath()).isEqualTo("/healthaccountservice/v1/ha/verify_mobile_otp");
        assertThat(captor.getValue().url().getHost()).isEqualTo("localhost");
    }

    @Test
    void shouldGetStateRequestResponse() throws JsonProcessingException {
        var state1 = StateRequestResponse.builder()
                .name("state1")
                .code("1")
                .districts(List.of(DistrictData.builder().code("1").name("district1").build()))
                .build();
        List <StateRequestResponse>stateList = new ArrayList<>();
        stateList.add(state1);
        String stateResponseJson = new ObjectMapper().writeValueAsString(stateList);

        when(exchangeFunction.exchange(captor.capture())).thenReturn(
                Mono.just(
                        ClientResponse
                                .create(HttpStatus.OK)
                                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                                .body(stateResponseJson)
                                .build()));

        StepVerifier.create(healthAccountServiceClient.getState()).assertNext(
                value -> {
                    assertThat(value.get(0).getCode()).isEqualTo("1");
                }
        ).verifyComplete();
    }
}