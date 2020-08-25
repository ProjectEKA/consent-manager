package in.projecteka.user;

import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.library.common.Authenticator;
import in.projecteka.library.common.Caller;
import in.projecteka.library.common.GatewayTokenVerifier;
import in.projecteka.library.common.RequestValidator;
import in.projecteka.library.common.ServiceCaller;
import in.projecteka.user.clients.UserServiceClient;
import in.projecteka.user.model.OtpVerification;
import in.projecteka.user.model.SignUpSession;
import in.projecteka.user.model.Token;
import in.projecteka.user.model.UserSignUpEnquiry;
import in.projecteka.user.properties.UserServiceProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static in.projecteka.library.common.Role.GATEWAY;
import static in.projecteka.user.Constants.PATH_FIND_PATIENT;
import static in.projecteka.user.TestBuilders.patientRequest;
import static in.projecteka.user.TestBuilders.string;
import static in.projecteka.user.TestBuilders.user;
import static java.lang.String.format;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.just;

@SuppressWarnings("ALL")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureWebTestClient
class UserControllerTest {

    @MockBean
    private UserService userService;

    @MockBean
    private SignUpService signupService;

    @Autowired
    private WebTestClient webClient;

    @SuppressWarnings("unused")
    @MockBean(name = "gatewayJWKSet")
    private JWKSet centralRegistryJWKSet;

    @SuppressWarnings("unused")
    @MockBean(name = "identityServiceJWKSet")
    private JWKSet identityServiceJWKSet;

    @MockBean
    private GatewayTokenVerifier gatewayTokenVerifier;

    @MockBean
    private Authenticator authenticator;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserServiceClient userServiceClient;

    @MockBean
    private RequestValidator validator;

    @MockBean
    private UserServiceProperties userServiceProperties;

    @Test
    void shouldReturnTemporarySessionIfOtpRequestIsSuccessful() {
        var userSignupEnquiry = new UserSignUpEnquiry("MOBILE", string());
        when(userService.sendOtp(any())).thenReturn(just(new SignUpSession(string())));

        webClient.post()
                .uri("/users/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(userSignupEnquiry))
                .exchange().expectStatus().isCreated();

        Mockito.verify(userService, times(1)).sendOtp(userSignupEnquiry);
    }

    @Test
    void shouldReturnTemporarySessionIfOtpPermitRequestIsSuccessful() {
        var otpVerification = new OtpVerification(string(), string());
        Token token = new Token(string());

        when(userService.verifyOtpForRegistration(any())).thenReturn(just(token));

        webClient.post()
                .uri("/users/permit")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(otpVerification))
                .exchange().expectStatus().isOk();

        Mockito.verify(userService, times(1)).verifyOtpForRegistration(otpVerification);
    }


    @Test
    void returnUser() {
        var username = string();
        var token = string();
        var sessionId = string();
        when(authenticator.verify(token)).thenReturn(just(new Caller(username, true)));
        when(userService.userWith(username)).thenReturn(just(user().build()));

        webClient.get()
                .uri(format("/internal/users/%s", username))
                .accept(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    void returnPatientResponseWhenUserFound() {
        var token = string();
        var patientRequest = patientRequest().build();
        var caller = ServiceCaller.builder().clientId("Client_ID").roles(List.of(GATEWAY)).build();
        when(validator.put(anyString(), any(LocalDateTime.class))).thenReturn(Mono.empty());
        when(validator.validate(anyString(), any(LocalDateTime.class))).thenReturn(Mono.just(Boolean.TRUE));
        when(gatewayTokenVerifier.verify(token)).thenReturn(just(caller));
        when(userService.user(patientRequest.getQuery().getPatient().getId(),
                patientRequest.getQuery().getRequester(),
                patientRequest.getRequestId()))
                .thenReturn(empty());

        webClient.post()
                .uri(PATH_FIND_PATIENT)
                .accept(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .body(BodyInserters.fromValue(patientRequest))
                .exchange()
                .expectStatus()
                .isAccepted();
    }

    @Test
    void shouldFailWithTooManyRequestsErrorForInvalidRequest() {
        var token = string();
        var patientRequest = patientRequest().build();
        var caller = ServiceCaller.builder().clientId("Client_ID").roles(List.of(GATEWAY)).build();

        when(validator.validate(anyString(), any(LocalDateTime.class))).thenReturn(Mono.just(Boolean.FALSE));
        when(gatewayTokenVerifier.verify(token)).thenReturn(just(caller));

        webClient.post()
                .uri(PATH_FIND_PATIENT)
                .accept(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .body(BodyInserters.fromValue(patientRequest))
                .exchange()
                .expectStatus()
                .is4xxClientError();
    }
}

