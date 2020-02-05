package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.user.model.OtpCommunicationData;
import in.projecteka.consentmanager.user.model.OtpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class OtpServiceClientTest {

    private @Captor
    ArgumentCaptor<ClientRequest> captor;
    @Mock
    private ExchangeFunction exchangeFunction;
    private OtpServiceClient otpServiceClient;
    private OtpRequest otpRequest;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        otpRequest = new OtpRequest(
                "SOME_SESSION_ID",
                new OtpCommunicationData("MOBILE", "1234567891")
        );
        WebClient.Builder webClientBuilder = WebClient.builder().exchangeFunction(exchangeFunction);
        final OtpServiceProperties otpServiceProperties
                = new OtpServiceProperties("localhost:8000/otpservice", Arrays.asList());
        otpServiceClient = new OtpServiceClient(webClientBuilder, otpServiceProperties);

    }

    @Test
    public void shouldReturnTemporaryTokenWhenOtpRequestSucceeds() {
        when(exchangeFunction.exchange(captor.capture())).thenReturn(Mono.just(ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", "application/json").build()));

        StepVerifier.create(otpServiceClient.sendOtpTo(otpRequest))
                .assertNext(response -> {
                    assertThat(response.getSessionId()).isEqualTo(otpRequest.getSessionId());
                });
    }

    @Test
    public void shouldThrowClientErrorWhenOtpRequestFails() {
        when(exchangeFunction.exchange(captor.capture())).thenReturn(Mono.just(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("Content-Type", "application/json").build()));

        StepVerifier.create(otpServiceClient.sendOtpTo(otpRequest))
                .expectErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().is5xxServerError())
                .verify();
    }
}
