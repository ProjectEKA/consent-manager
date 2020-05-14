package in.projecteka.consentmanager.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.consentmanager.clients.model.KeyCloakUserPasswordChangeRequest;
import in.projecteka.consentmanager.clients.model.KeyCloakUserRepresentation;
import in.projecteka.consentmanager.clients.model.Session;
import in.projecteka.consentmanager.clients.model.ErrorCode;
import org.junit.Assert;
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

import java.util.HashMap;
import java.util.Map;

import static in.projecteka.consentmanager.clients.TestBuilders.keycloakCreateUser;
import static in.projecteka.consentmanager.clients.TestBuilders.keycloakProperties;
import static in.projecteka.consentmanager.clients.TestBuilders.session;
import static in.projecteka.consentmanager.clients.TestBuilders.string;
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
    public void shouldThrowExceptionForExpiredOTP() throws JsonProcessingException {
        when(exchangeFunction.exchange(captor.capture())).thenReturn(
                Mono.just(ClientResponse
                        .create(HttpStatus.UNAUTHORIZED)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(InvalidOtpResponse("1003","Invalid Otp"))
                        .build()));
        StepVerifier.create(identityServiceClient.getToken(formData))
                .expectErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().is4xxClientError() &&
                        ((ClientError) throwable).getError().getError().getCode().equals(ErrorCode.OTP_EXPIRED))
                .verify();
    }

    @Test
    public void shouldThrowExceptionForIncorrectOTP() throws JsonProcessingException {
        when(exchangeFunction.exchange(captor.capture())).thenReturn(
                Mono.just(ClientResponse
                        .create(HttpStatus.UNAUTHORIZED)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(InvalidOtpResponse("1002", "Invalid Otp"))
                        .build()));
        StepVerifier.create(identityServiceClient.getToken(formData))
                .expectErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().is4xxClientError() &&
                        ((ClientError) throwable).getError().getError().getCode().equals(ErrorCode.OTP_INVALID))
                .verify();
    }

    private static String InvalidOtpResponse(String error, String description) throws JsonProcessingException {
        Map<String, String> map = new HashMap<>(2);
        map.put("error",error);
        map.put("error_description",description);
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(map);
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
    public void shouldGetUserFromKeyCloak() throws JsonProcessingException {
        var userName = string();
        var accessToken = string();
        KeyCloakUserRepresentation keyCloakUserRepresentation = KeyCloakUserRepresentation.builder()
                .id("userid").build();
        String getUserResponseBody = new ObjectMapper().writeValueAsString(keyCloakUserRepresentation);

        when(exchangeFunction.exchange(captor.capture()))
                .thenReturn(Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", "application/json")
                        .body(getUserResponseBody).build()));

        StepVerifier.create(identityServiceClient.getUser(userName, accessToken))
                .assertNext(cloakUserRepresentation -> assertThat(keyCloakUserRepresentation.getId().equals(cloakUserRepresentation.getId())))
                .verifyComplete();
        assertThat(captor.getValue().headers().get(HttpHeaders.AUTHORIZATION).get(0)).isEqualTo(accessToken);
    }

    @Test
    public void shouldGetUserFailsFromKeyCloak() throws JsonProcessingException {
        var userName = string();
        var accessToken = string();

        when(exchangeFunction.exchange(captor.capture())).thenReturn(
                Mono.just(ClientResponse
                        .create(HttpStatus.NOT_FOUND)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .build()));

        StepVerifier.create(identityServiceClient.getUser(userName, accessToken))
                .expectErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().is4xxClientError())
                .verify();
        assertThat(captor.getValue().headers().get(HttpHeaders.AUTHORIZATION).get(0)).isEqualTo(accessToken);

    }

    @Test
    public void shouldUpdateUserInKeyCloak() throws JsonProcessingException {
        var session = Session.builder().build();
        var userPwd = "Test@325";
        var keyCloakUserId = "userId";
        KeyCloakUserPasswordChangeRequest keyCloakUserPasswordChangeRequest = KeyCloakUserPasswordChangeRequest
                .builder()
                .value(userPwd)
                .build();
        String updateUserResponseBody = new ObjectMapper().writeValueAsString(keyCloakUserPasswordChangeRequest);

        when(exchangeFunction.exchange(captor.capture()))
                .thenReturn(Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", "application/json")
                        .body(updateUserResponseBody).build()));

        StepVerifier.create(identityServiceClient.updateUser(session, keyCloakUserId, userPwd))
                .verifyComplete();
        assertThat(captor.getValue().headers().get(HttpHeaders.AUTHORIZATION).get(0)).isEqualTo("Bearer " +session.getAccessToken());
    }

    @Test
    public void shouldReturnErrorWhenUserNotFoundWhileUpdatingUserInKeyCloak() throws JsonProcessingException {
        var session = Session.builder().build();
        var userPwd = "Test@325";
        var keyCloakUserId = "userId";
        KeyCloakUserPasswordChangeRequest keyCloakUserPasswordChangeRequest = KeyCloakUserPasswordChangeRequest
                .builder()
                .value(userPwd)
                .build();
        String updateUserResponseBody = new ObjectMapper().writeValueAsString(keyCloakUserPasswordChangeRequest);

        when(exchangeFunction.exchange(captor.capture()))
                .thenReturn(Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND)
                        .header("Content-Type", "application/json")
                        .body(updateUserResponseBody).build()));

        StepVerifier.create(identityServiceClient.updateUser(session, keyCloakUserId, userPwd))
                .expectErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().is4xxClientError())
                .verify();
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

    @Test
    public void shouldLogout() {
        when(exchangeFunction.exchange(captor.capture())).
                thenReturn(Mono.just(ClientResponse.create(HttpStatus.OK).build()));

        StepVerifier.create(identityServiceClient.logout(formData))
                .verifyComplete();
        Assert.assertTrue(captor.getValue().url().toString().endsWith("realms/consent-manager/protocol/openid-connect/logout"));
    }

    @Test
    public void shouldThrowErrorIfUpstreamLogoutRequestFails() {
        when(exchangeFunction.exchange(captor.capture())).thenReturn(
                Mono.just(ClientResponse
                        .create(HttpStatus.INTERNAL_SERVER_ERROR)
                        .build()));

        StepVerifier.create(identityServiceClient.logout(formData))
                .expectErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().is5xxServerError())
                .verify();
        Assert.assertTrue(captor.getValue().url().toString().endsWith("realms/consent-manager/protocol/openid-connect/logout"));
    }
}