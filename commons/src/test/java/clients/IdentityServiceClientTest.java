package clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import in.projecteka.consentmanager.clients.IdentityServiceClient;
import in.projecteka.consentmanager.clients.model.ClientError;
import in.projecteka.consentmanager.clients.model.ErrorCode;
import in.projecteka.consentmanager.clients.model.KeyCloakUserCredentialRepresentation;
import in.projecteka.consentmanager.clients.model.KeyCloakUserPasswordChangeRequest;
import in.projecteka.consentmanager.clients.model.KeyCloakUserRepresentation;
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

import static clients.TestBuilders.keyCloakUserPasswordChangeRequest;
import static clients.TestBuilders.keycloakCreateUser;
import static clients.TestBuilders.session;
import static common.TestBuilders.OBJECT_MAPPER;
import static common.TestBuilders.string;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        identityServiceClient = new IdentityServiceClient(webClientBuilder, "");
    }

    @Test
    void shouldThrowExceptionIfTokenCallFails() {
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
    void shouldThrowExceptionForExpiredOTP() throws JsonProcessingException {
        when(exchangeFunction.exchange(captor.capture())).thenReturn(
                Mono.just(ClientResponse
                        .create(HttpStatus.UNAUTHORIZED)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(InvalidOtpResponse("1003"))
                        .build()));
        StepVerifier.create(identityServiceClient.getToken(formData))
                .expectErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().is4xxClientError() &&
                        ((ClientError) throwable).getError().getError().getCode().equals(ErrorCode.OTP_EXPIRED))
                .verify();
    }

    @Test
    void shouldThrowExceptionForIncorrectOTP() throws JsonProcessingException {
        when(exchangeFunction.exchange(captor.capture())).thenReturn(
                Mono.just(ClientResponse
                        .create(HttpStatus.UNAUTHORIZED)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(InvalidOtpResponse("1002"))
                        .build()));
        StepVerifier.create(identityServiceClient.getToken(formData))
                .expectErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().is4xxClientError() &&
                        ((ClientError) throwable).getError().getError().getCode().equals(ErrorCode.OTP_INVALID))
                .verify();
    }

    private static String InvalidOtpResponse(String error) throws JsonProcessingException {
        Map<String, String> map = new HashMap<>(2);
        map.put("error", error);
        map.put("error_description", "Invalid Otp");
        return OBJECT_MAPPER.writeValueAsString(map);
    }

    @Test
    void shouldReturnAccessTokenForUser() {
        when(exchangeFunction.exchange(captor.capture())).thenReturn(
                Mono.just(ClientResponse
                        .create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .build()));

        StepVerifier.create(identityServiceClient.getToken(formData))
                .verifyComplete();
    }

    @Test
    void shouldCreateUserAccountInKeycloak() {
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
    void shouldGetUserFromKeyCloak() throws JsonProcessingException {
        var userName = string();
        var accessToken = string();
        KeyCloakUserRepresentation keyCloakUserRepresentation = KeyCloakUserRepresentation.builder()
                .id("userid").build();
        String getUserResponseBody = OBJECT_MAPPER.writeValueAsString(keyCloakUserRepresentation);

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
    void shouldGetUserFailsFromKeyCloak() {
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
    void shouldUpdateUserInKeyCloak() throws JsonProcessingException {
        var userPwd = "Test@325";
        var keyCloakUserId = "userId";
        String accessToken = "Bearer " + string();
        KeyCloakUserPasswordChangeRequest keyCloakUserPasswordChangeRequest = keyCloakUserPasswordChangeRequest()
                .value(userPwd)
                .build();
        String updateUserResponseBody = OBJECT_MAPPER.writeValueAsString(keyCloakUserPasswordChangeRequest);
        when(exchangeFunction.exchange(captor.capture()))
                .thenReturn(Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", "application/json")
                        .body(updateUserResponseBody).build()));

        var publisher = identityServiceClient.updateUser(accessToken, keyCloakUserId, userPwd);

        StepVerifier.create(publisher).verifyComplete();
        assertThat(captor.getValue().headers().get(HttpHeaders.AUTHORIZATION).get(0)).isEqualTo(accessToken);
    }

    @Test
    void shouldReturnErrorWhenUserNotFoundWhileUpdatingUserInKeyCloak() throws JsonProcessingException {
        String accessToken = "Bearer " + string();
        var userPwd = "Test@325";
        var keyCloakUserId = "userId";
        var keyCloakUserPasswordChangeRequest = keyCloakUserPasswordChangeRequest()
                .value(userPwd)
                .build();
        String updateUserResponseBody = OBJECT_MAPPER.writeValueAsString(keyCloakUserPasswordChangeRequest);
        when(exchangeFunction.exchange(captor.capture()))
                .thenReturn(Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND)
                        .header("Content-Type", "application/json")
                        .body(updateUserResponseBody).build()));

        var publisher = identityServiceClient.updateUser(accessToken, keyCloakUserId, userPwd);

        StepVerifier.create(publisher)
                .expectErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().is4xxClientError())
                .verify();
    }

    @Test
    void shouldThrowExceptionIfUserCreationRequestFails() {
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
    void shouldLogout() {
        when(exchangeFunction.exchange(captor.capture())).
                thenReturn(Mono.just(ClientResponse.create(HttpStatus.OK).build()));

        StepVerifier.create(identityServiceClient.logout(formData))
                .verifyComplete();
        assertTrue(captor.getValue().url().toString().endsWith("realms/consent-manager/protocol/openid-connect/logout"));
    }

    @Test
    void shouldThrowErrorIfUpstreamLogoutRequestFails() {
        when(exchangeFunction.exchange(captor.capture())).thenReturn(
                Mono.just(ClientResponse
                        .create(HttpStatus.INTERNAL_SERVER_ERROR)
                        .build()));

        StepVerifier.create(identityServiceClient.logout(formData))
                .expectErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().is5xxServerError())
                .verify();
        assertTrue(captor.getValue().url().toString().endsWith("realms/consent-manager/protocol/openid-connect/logout"));
    }

    @Test
    void getCredentials() throws JsonProcessingException {
        var userName = string();
        var accessToken = string();
        KeyCloakUserCredentialRepresentation keyCreds = KeyCloakUserCredentialRepresentation.builder()
                .id("credid").build();
        String getUserResponseBody = OBJECT_MAPPER.writeValueAsString(keyCreds);

        when(exchangeFunction.exchange(captor.capture()))
                .thenReturn(Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", "application/json")
                        .body(getUserResponseBody).build()));

        StepVerifier.create(identityServiceClient.getCredentials(userName, accessToken))
                .assertNext(keyCloakUserCredentialRepresentation ->
                        assertThat(keyCloakUserCredentialRepresentation.getId().equals(keyCreds.getId())))
                .verifyComplete();
        assertThat(captor.getValue().headers().get(HttpHeaders.AUTHORIZATION).get(0)).isEqualTo(accessToken);
    }

    @Test
    void getCredentialsFailsFromKeyCloak() {
        var userName = string();
        var accessToken = string();

        when(exchangeFunction.exchange(captor.capture())).thenReturn(
                Mono.just(ClientResponse
                        .create(HttpStatus.NOT_FOUND)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .build()));

        StepVerifier.create(identityServiceClient.getCredentials(userName, accessToken))
                .expectErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().is4xxClientError())
                .verify();
        assertThat(captor.getValue().headers().get(HttpHeaders.AUTHORIZATION).get(0)).isEqualTo(accessToken);
    }
}