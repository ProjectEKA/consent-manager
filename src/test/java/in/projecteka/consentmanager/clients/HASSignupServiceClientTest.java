package in.projecteka.consentmanager.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.consentmanager.user.TestBuilders;
import in.projecteka.consentmanager.user.model.HASSignupRequest;
import in.projecteka.consentmanager.user.model.HealthAccountUser;
import in.projecteka.consentmanager.user.model.UpdateHASUserRequest;
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

import static in.projecteka.consentmanager.user.Constants.HAS_ACCOUNT_UPDATE;
import static in.projecteka.consentmanager.user.Constants.HAS_CREATE_ACCOUNT_VERIFIED_MOBILE_TOKEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class HASSignupServiceClientTest {
    private @Captor
    ArgumentCaptor<ClientRequest> captor;
    @Mock
    private ExchangeFunction exchangeFunction;
    private HASSignupServiceClient hasSignupServiceClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        WebClient.Builder webClientBuilder = WebClient.builder().exchangeFunction(exchangeFunction);
        hasSignupServiceClient = new HASSignupServiceClient(webClientBuilder, "http://localhost/healthaccountservice");
    }

    @Test
    public void shouldCreateHealthAccountForUser() throws JsonProcessingException {
        HASSignupRequest hasSignupRequest = TestBuilders.hasSignupRequest().build();

        HealthAccountUser healthAccountUser = TestBuilders.healthAccountUser().build();
        String jsonResponse = new ObjectMapper().writeValueAsString(healthAccountUser);

        when(exchangeFunction.exchange(captor.capture())).thenReturn(
                Mono.just(
                        ClientResponse
                                .create(HttpStatus.OK)
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .body(jsonResponse)
                                .build()));

        StepVerifier.create(hasSignupServiceClient.createHASAccount(hasSignupRequest))
                .assertNext(actualHealthAccountUser -> {
                    assertThat(actualHealthAccountUser).isEqualTo(healthAccountUser);
                })
                .verifyComplete();

        assertThat(captor.getValue().url().getPath()).isEqualTo("/healthaccountservice"+HAS_CREATE_ACCOUNT_VERIFIED_MOBILE_TOKEN);
        assertThat(captor.getValue().url().getHost()).isEqualTo("localhost");
    }

    @Test
    public void shouldUpdateHealthAccountWithGivenToken() throws JsonProcessingException {
        String token = "Token";
        UpdateHASUserRequest updateHASUserRequest = TestBuilders.updateHASUserRequestBuilder().build();

        when(exchangeFunction.exchange(captor.capture())).thenReturn(
                Mono.just(
                        ClientResponse
                                .create(HttpStatus.OK)
                                .build()));

        StepVerifier.create(hasSignupServiceClient.updateHASAccount(updateHASUserRequest, token))
                .verifyComplete();

        assertThat(captor.getValue().url().getPath()).isEqualTo("/healthaccountservice"+HAS_ACCOUNT_UPDATE);
        assertThat(captor.getValue().url().getHost()).isEqualTo("localhost");
        assertThat(captor.getValue().headers().getFirst("X-Token")).isEqualTo(token);
    }
}