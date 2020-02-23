package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.user.KeycloakProperties;
import in.projecteka.consentmanager.user.model.KeycloakCreateUserRequest;
import in.projecteka.consentmanager.user.model.KeycloakToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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

import java.util.Collections;

import static org.mockito.Mockito.when;

class KeycloakClientTest {

    private @Captor
    ArgumentCaptor<ClientRequest> captor;
    @Mock
    private ExchangeFunction exchangeFunction;
    private KeycloakClient keycloakClient;
    private MultiValueMap<String, String> formData= new LinkedMultiValueMap<>();
    private KeycloakCreateUserRequest request;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        request = new KeycloakCreateUserRequest(
              "SOME_FIRST_NAME",
              "SOME_LAST_NAME",
              "SOME_USER_NAME",
              Collections.emptyList(),
                "true"
        );
        final KeycloakProperties keycloakProperties
                = new KeycloakProperties("http://localhost:8080/auth",
                "SOME_CLIENT_ID",
                "CLIENT_SECRET",
                "SOME_USER_NAME",
                "PASSWORD");
        WebClient.Builder webClientBuilder = WebClient.builder().exchangeFunction(exchangeFunction);
        keycloakClient = new KeycloakClient(webClientBuilder, keycloakProperties);
    }

    @Test
    public void shouldThrowExceptionIfTokenCallFails() {
        when(exchangeFunction.exchange(captor.capture())).thenReturn(
                Mono.just(
                        ClientResponse
                                .create(HttpStatus.INTERNAL_SERVER_ERROR)
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .build()));



        StepVerifier.create(keycloakClient.getToken(formData))
                .expectErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().is5xxServerError())
                .verify();
    }

    @Test
    public void shouldReturnAccessTokenForUser() {

        when(exchangeFunction.exchange(captor.capture())).thenReturn(
                Mono.just(
                        ClientResponse
                                .create(HttpStatus.OK)
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .build()));

        StepVerifier.create(keycloakClient.getToken(formData))
                .verifyComplete();
    }

    @Test
    public void shouldCreateUserAccountInKeycloak() {
        when(exchangeFunction.exchange(captor.capture())).thenReturn(
                Mono.just(
                        ClientResponse
                                .create(HttpStatus.OK)
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .build()));

        StepVerifier.create(keycloakClient.createUser(new KeycloakToken(), request))
                .verifyComplete();
    }

    @Test
    public void shouldThrowExceptionIfUserCreationRequestFails() {

        when(exchangeFunction.exchange(captor.capture())).thenReturn(
                Mono.just(
                        ClientResponse
                                .create(HttpStatus.INTERNAL_SERVER_ERROR)
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .build()));



        StepVerifier.create(keycloakClient.createUser(new KeycloakToken(), request))
                .expectErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().is5xxServerError())
                .verify();
    }

}