package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.OtpServiceClient;
import in.projecteka.consentmanager.clients.model.OtpCommunicationData;
import in.projecteka.consentmanager.clients.model.OtpRequest;
import in.projecteka.consentmanager.clients.properties.OtpServiceProperties;
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

import java.util.Collections;

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

        final OtpServiceProperties otpServiceProperties
                = new OtpServiceProperties("localhost:8000/otpservice", Collections.emptyList());
        WebClient.Builder webClientBuilder = WebClient.builder().exchangeFunction(exchangeFunction);
        otpServiceClient = new OtpServiceClient(webClientBuilder, otpServiceProperties);
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

        StepVerifier.create(otpServiceClient.send(otpRequest))
                .verifyComplete();
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
    }
}
