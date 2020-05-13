package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.OtpServiceClient;
import in.projecteka.consentmanager.clients.model.OtpCommunicationData;
import in.projecteka.consentmanager.clients.model.OtpRequest;
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

class OtpServiceClientTest {

    private @Captor
    ArgumentCaptor<ClientRequest> captor;
    @Mock
    private ExchangeFunction exchangeFunction;
    private OtpServiceClient otpServiceClient;

    private EasyRandom easyRandom;

    @BeforeEach
    void setUp() {
        easyRandom = new EasyRandom();
        MockitoAnnotations.initMocks(this);
        WebClient.Builder webClientBuilder = WebClient.builder().exchangeFunction(exchangeFunction);
        otpServiceClient = new OtpServiceClient(webClientBuilder, "http://localhost/otpservice");
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
                                .build()));

        StepVerifier.create(otpServiceClient.send(otpRequest)).verifyComplete();
        assertThat(captor.getValue().url().getPath()).isEqualTo("/otpservice/otp");
        assertThat(captor.getValue().url().getHost()).isEqualTo("localhost");
    }

    @Test
    public void shouldThrowClientErrorWhenOtpRequestFails() {
        var otpRequest = new OtpRequest(easyRandom.nextObject(String.class),
                new OtpCommunicationData("MOBILE", easyRandom.nextObject(String.class)));
        when(exchangeFunction.exchange(captor.capture())).thenReturn(
                Mono.just(
                        ClientResponse
                                .create(HttpStatus.INTERNAL_SERVER_ERROR)
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .build()));

        StepVerifier.create(otpServiceClient.send(otpRequest))
                .expectErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().is5xxServerError())
                .verify();
        assertThat(captor.getValue().url().getPath()).isEqualTo("/otpservice/otp");
        assertThat(captor.getValue().url().getHost()).isEqualTo("localhost");
    }

    @Test
    public void shouldVerifyOtp() {
        var sessionId = easyRandom.nextObject(String.class);
        var otp = easyRandom.nextObject(String.class);

        when(exchangeFunction.exchange(captor.capture())).thenReturn(
                Mono.just(ClientResponse
                        .create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .build()));

        StepVerifier.create(otpServiceClient.verify(sessionId, otp)).verifyComplete();
        assertThat(captor.getValue().url().getPath()).isEqualTo("/otpservice/otp/" + sessionId + "/verify");
        assertThat(captor.getValue().url().getHost()).isEqualTo("localhost");
    }

    @Test
    public void shouldThrowUnauthorizedWhenOtpExpired() {
        var sessionId = easyRandom.nextObject(String.class);
        var otp = easyRandom.nextObject(String.class);

        when(exchangeFunction.exchange(captor.capture())).thenReturn(
                Mono.just(ClientResponse
                        .create(HttpStatus.UNAUTHORIZED)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .build()));

        StepVerifier.create(otpServiceClient.verify(sessionId, otp))
                .expectErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().value() == 401)
                .verify();
        assertThat(captor.getValue().url().getPath()).isEqualTo("/otpservice/otp/" + sessionId + "/verify");
        assertThat(captor.getValue().url().getHost()).isEqualTo("localhost");
    }

    @Test
    public void shouldThrowBadRequestWhenOtpIsInvalid() {
        var sessionId = easyRandom.nextObject(String.class);
        var otp = easyRandom.nextObject(String.class);

        when(exchangeFunction.exchange(captor.capture())).thenReturn(
                Mono.just(ClientResponse
                        .create(HttpStatus.UNAUTHORIZED)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .build()));

        StepVerifier.create(otpServiceClient.verify(sessionId, otp))
                .expectErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().value() == 401)
                .verify();
        assertThat(captor.getValue().url().getPath()).isEqualTo("/otpservice/otp/" + sessionId + "/verify");
        assertThat(captor.getValue().url().getHost()).isEqualTo("localhost");
    }
}
