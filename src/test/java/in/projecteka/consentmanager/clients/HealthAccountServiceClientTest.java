package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.model.OtpRequestResponse;
import in.projecteka.consentmanager.clients.model.OtpCommunicationData;
import in.projecteka.consentmanager.clients.model.OtpRequest;
import in.projecteka.consentmanager.clients.model.HealthAccountServiceTokenResponse;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

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
        var otpRequest = new OtpRequest(sessionId, new OtpCommunicationData("MOBILE", value));

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
}