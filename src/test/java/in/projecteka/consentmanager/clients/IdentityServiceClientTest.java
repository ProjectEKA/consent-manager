package in.projecteka.consentmanager.clients;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static in.projecteka.consentmanager.clients.TestBuilders.keycloakCreateUser;
import static in.projecteka.consentmanager.clients.TestBuilders.keycloakProperties;
import static in.projecteka.consentmanager.clients.TestBuilders.session;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class IdentityServiceClientTest {

    private @Captor
    ArgumentCaptor<ClientRequest> captor;
    @Mock
    private ExchangeFunction exchangeFunction;
    private IdentityServiceClient identityServiceClient;
    private final MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

    @BeforeEach
    void setUp() {
        initMocks(this);
        WebClient.Builder webClientBuilder = WebClient.builder().exchangeFunction(exchangeFunction);
        identityServiceClient = new IdentityServiceClient(webClientBuilder, keycloakProperties().build());
    }

    @Test
    public void shouldThrowExceptionIfTokenCallFails() {
        when(exchangeFunction.exchange(captor.capture())).thenReturn(
                Mono.just(ClientResponse
                        .create(HttpStatus.INTERNAL_SERVER_ERROR)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .build()));

        StepVerifier.create(identityServiceClient.getToken(formData))
                .expectErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().is5xxServerError())
                .verify();
    }

    @Test
    public void shouldReturnAccessTokenForUser() {
        when(exchangeFunction.exchange(captor.capture())).thenReturn(
                Mono.just(ClientResponse
                        .create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .build()));

        StepVerifier.create(identityServiceClient.getToken(formData))
                .verifyComplete();
    }

    @Test
    public void shouldCreateUserAccountInKeycloak() {
        var request = keycloakCreateUser().build();
        var token = session().build();
        when(exchangeFunction.exchange(captor.capture())).thenReturn(
                Mono.just(ClientResponse
                        .create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .build()));

        StepVerifier.create(identityServiceClient.createUser(token, request))
                .verifyComplete();
        assertThat(captor.getValue().headers().get(HttpHeaders.AUTHORIZATION).get(0))
                .isEqualTo(format("Bearer %s", token.getAccessToken()));
    }

    @Test
    public void shouldThrowExceptionIfUserCreationRequestFails() {
        var request = keycloakCreateUser().build();
        var token = session().build();
        when(exchangeFunction.exchange(captor.capture())).thenReturn(
                Mono.just(ClientResponse
                        .create(HttpStatus.INTERNAL_SERVER_ERROR)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .build()));

        StepVerifier.create(identityServiceClient.createUser(token, request))
                .expectErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().is5xxServerError())
                .verify();
        assertThat(captor.getValue().headers().get(HttpHeaders.AUTHORIZATION).get(0))
                .isEqualTo(format("Bearer %s", token.getAccessToken()));
    }
}