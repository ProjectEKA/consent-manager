package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.model.HASOtpRequestResponse;
import in.projecteka.consentmanager.clients.model.OtpCommunicationData;
import in.projecteka.consentmanager.clients.model.OtpRequest;
import org.jeasy.random.EasyRandom;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class HASServiceClientTest {
    private @Captor
    ArgumentCaptor<ClientRequest> captor;
    @Mock
    private ExchangeFunction exchangeFunction;
    private HASServiceClient hasServiceClient;
    private EasyRandom easyRandom;

    @BeforeEach
    void setUp() {
        easyRandom = new EasyRandom();

        MockitoAnnotations.initMocks(this);
        WebClient.Builder webClientBuilder = WebClient.builder().exchangeFunction(exchangeFunction);
        hasServiceClient = new HASServiceClient(webClientBuilder, "http://localhost/hasservice");
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

        Mono<HASOtpRequestResponse> response = hasServiceClient.send(otpRequest);

        StepVerifier.create(response).assertNext(
                hasOtpRequestResponse -> assertThat(hasOtpRequestResponse.getTxnID()).isEqualTo("12345")
        ).verifyComplete();

        assertThat(captor.getValue().url().getPath()).isEqualTo("/hasservice/v1/ha/generate_mobile_otp");
        assertThat(captor.getValue().url().getHost()).isEqualTo("localhost");

        //TODO: Assert Request Body
    }
}