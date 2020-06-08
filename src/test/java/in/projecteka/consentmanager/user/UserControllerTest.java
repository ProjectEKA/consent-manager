package in.projecteka.consentmanager.user;

import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.common.Authenticator;
import in.projecteka.consentmanager.common.Caller;
import in.projecteka.consentmanager.common.CentralRegistryTokenVerifier;
import in.projecteka.consentmanager.consent.ConceptValidator;
import in.projecteka.consentmanager.consent.ConsentRequestNotificationListener;
import in.projecteka.consentmanager.consent.HipConsentNotificationListener;
import in.projecteka.consentmanager.consent.HiuConsentNotificationListener;
import in.projecteka.consentmanager.dataflow.DataFlowBroadcastListener;
import in.projecteka.consentmanager.user.model.OtpVerification;
import in.projecteka.consentmanager.user.model.RequesterDetail;
import in.projecteka.consentmanager.user.model.SignUpSession;
import in.projecteka.consentmanager.user.model.Token;
import in.projecteka.consentmanager.user.model.UserSignUpEnquiry;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import static in.projecteka.consentmanager.user.TestBuilders.patientRequest;
import static in.projecteka.consentmanager.user.TestBuilders.string;
import static in.projecteka.consentmanager.user.TestBuilders.user;
import static java.lang.String.format;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@SuppressWarnings("ALL")

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureWebTestClient
class UserControllerTest {

    @MockBean
    private UserService userService;

    @MockBean
    private DestinationsConfig destinationsConfig;

    @MockBean
    private HiuConsentNotificationListener hiuConsentNotificationListener;

    @MockBean
    private HipConsentNotificationListener hipConsentNotificationListener;

    @MockBean
    private DataFlowBroadcastListener dataFlowBroadcastListener;

    @SuppressWarnings("unused")
    @MockBean
    private ConsentRequestNotificationListener consentRequestNotificationListener;

    @MockBean
    private SignUpService signupService;

    @Autowired
    private WebTestClient webClient;

    @SuppressWarnings("unused")
    @MockBean(name = "centralRegistryJWKSet")
    private JWKSet centralRegistryJWKSet;

    @SuppressWarnings("unused")
    @MockBean(name = "identityServiceJWKSet")
    private JWKSet identityServiceJWKSet;

    @MockBean
    private CentralRegistryTokenVerifier centralRegistryTokenVerifier;

    @MockBean
    private Authenticator authenticator;

    @SuppressWarnings("unused")
    @MockBean
    private ConceptValidator conceptValidator;

    @MockBean
    private RequesterDetail requester;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserServiceClient userServiceClient;

    @Test
    public void shouldReturnTemporarySessionIfOtpRequestIsSuccessful() {
        var userSignupEnquiry = new UserSignUpEnquiry("MOBILE", string());
        when(userService.sendOtp(any())).thenReturn(Mono.just(new SignUpSession(string())));

        webClient.post()
                .uri("/users/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(userSignupEnquiry))
                .exchange().expectStatus().isCreated();

        Mockito.verify(userService, times(1)).sendOtp(userSignupEnquiry);
    }

    @Test
    public void shouldReturnTemporarySessionIfOtpPermitRequestIsSuccessful() {
        var otpVerification = new OtpVerification(string(), string());
        Token token = new Token(string());

        when(userService.verifyOtpForRegistration(any())).thenReturn(Mono.just(token));

        webClient.post()
                .uri("/users/permit")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(otpVerification))
                .exchange().expectStatus().isOk();

        Mockito.verify(userService, times(1)).verifyOtpForRegistration(otpVerification);
    }

    @Test
    public void returnUserForCentralRegistryAuthenticatedSystem() {
        var username = string();
        var token = string();
        var sessionId = string();
        when(centralRegistryTokenVerifier.verify(token)).thenReturn(Mono.just(new Caller(username, false)));
        when(userService.userWith(username)).thenReturn(Mono.just(user().build()));

        webClient.get()
                .uri(format("/users/%s", username))
                .accept(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    public void returnUser() {
        var username = string();
        var token = string();
        var sessionId = string();
        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller(username, true)));
        when(userService.userWith(username)).thenReturn(Mono.just(user().build()));

        webClient.get()
                .uri(format("/internal/users/%s", username))
                .accept(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    public void returnPatientResponseWhenUserFound() {
        var token = string();
        var patientRequest = patientRequest().build();
        var userName = patientRequest.getQuery().getRequester().getId();
        var user = user().identifier(userName).build();
        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller(userName, false)));
        when(requester.getType()).thenReturn("HIU");
        when(userRepository.userWith(any())).thenReturn(Mono.just(user));
        when(userServiceClient.sendPatientResponseToGateWay(any(),
                eq("X-HIU-ID"),
                eq(patientRequest.getQuery().getRequester().getId())))
                .thenReturn(Mono.empty());

        webClient.post()
                .uri("/v1/patients/find")
                .accept(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .body(BodyInserters.fromValue(patientRequest))
                .exchange()
                .expectStatus()
                .isAccepted();
    }
}

