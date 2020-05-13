package in.projecteka.consentmanager.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.consent.ConceptValidator;
import in.projecteka.consentmanager.clients.model.Session;
import in.projecteka.consentmanager.consent.ConsentRequestNotificationListener;
import in.projecteka.consentmanager.consent.HipConsentNotificationListener;
import in.projecteka.consentmanager.consent.HiuConsentNotificationListener;
import in.projecteka.consentmanager.dataflow.DataFlowBroadcastListener;
import in.projecteka.consentmanager.user.model.CoreSignUpRequest;
import in.projecteka.consentmanager.user.model.GenerateOtpRequest;
import in.projecteka.consentmanager.user.model.GenerateOtpResponse;
import in.projecteka.consentmanager.user.model.Identifier;
import in.projecteka.consentmanager.user.model.IdentifierType;
import in.projecteka.consentmanager.user.model.OtpMediumType;
import in.projecteka.consentmanager.user.model.OtpVerification;
import in.projecteka.consentmanager.user.model.Profile;
import in.projecteka.consentmanager.user.model.SignUpSession;
import in.projecteka.consentmanager.user.model.Token;
import in.projecteka.consentmanager.user.model.UpdateUserRequest;
import in.projecteka.consentmanager.user.model.UserSignUpEnquiry;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static in.projecteka.consentmanager.user.TestBuilders.coreSignUpRequest;
import static in.projecteka.consentmanager.user.TestBuilders.session;
import static in.projecteka.consentmanager.user.TestBuilders.string;
import static java.time.LocalDate.now;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@SuppressWarnings("unused")
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureWebTestClient
public class
PatientControllerTest {

    @MockBean
    private UserService userService;

    @MockBean
    private ProfileService profileService;

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

    @SuppressWarnings("unused")
    @MockBean
    private ConceptValidator conceptValidator;

    @Test
    public void createUser() {
        var signUpRequest = coreSignUpRequest()
                .username("username@ncg")
                .name("RandomName")
                .password("@2Abaafasfas")
                .yearOfBirth(now().getYear())
                .build();
        var token = string();
        var sessionId = string();
        var session = session().build();
        when(signupService.sessionFrom(token)).thenReturn(sessionId);
        when(userService.create(any(CoreSignUpRequest.class), eq(sessionId))).thenReturn(Mono.just(session));
        when(userService.getUserIdSuffix()).thenReturn("@ncg");
        when(signupService.validateToken(token)).thenReturn(Mono.just(true));
        when(signupService.removeOf(sessionId)).thenReturn(Mono.empty());

        webClient.post()
                .uri("/patients/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .body(BodyInserters.fromValue(signUpRequest))
                .exchange().expectStatus().isOk();
    }

    @Test
    public void returnBadRequestForUserCreation() {
        var signUpRequest = coreSignUpRequest()
                .name("RandomName")
                .yearOfBirth(now().plusDays(1).getYear())
                .build();
        var token = string();
        var sessionId = string();
        var session = session().build();
        when(signupService.sessionFrom(token)).thenReturn(sessionId);
        when(userService.create(signUpRequest, sessionId)).thenReturn(Mono.just(session));
        when(userService.getUserIdSuffix()).thenReturn("@ncg");
        when(signupService.validateToken(token)).thenReturn(Mono.just(true));

        webClient.post()
                .uri("/patients/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .body(BodyInserters.fromValue(signUpRequest))
                .exchange()
                .expectStatus()
                .is4xxClientError();
    }

    @Test
    public void generateOtp() throws JsonProcessingException {
        GenerateOtpRequest otpRequest = GenerateOtpRequest.builder().username("username@ncg").build();
        List<Identifier> identifiers = new ArrayList<Identifier>();
        Identifier identifier = Identifier.builder()
                .type(IdentifierType.MOBILE)
                .value("9999999999")
                .build();
        identifiers.add(identifier);
        Profile profile = Profile.builder()
                .verifiedIdentifiers(identifiers)
                .build();
        UserSignUpEnquiry userSignUpEnquiry = UserSignUpEnquiry.builder()
                .identifierType(identifier.getType().toString())
                .identifier(identifier.getValue())
                .build();
        SignUpSession signUpSession = new SignUpSession("sessionId");
        GenerateOtpResponse expectedResponse = GenerateOtpResponse.builder()
                .otpMedium(OtpMediumType.MOBILE.toString())
                .otpMediumValue("******9999")
                .sessionId(signUpSession.getSessionId())
                .build();
        var expectedResponseJson = new ObjectMapper().writeValueAsString(expectedResponse);

        when(profileService.profileFor(otpRequest.getUsername())).thenReturn(Mono.just(profile));
        when(userService.sendOtpForPasswordChange(userSignUpEnquiry, otpRequest.getUsername())).thenReturn(Mono.just(signUpSession));

        webClient.post()
                .uri("/patients/generateotp")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(otpRequest))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody()
                .json(expectedResponseJson);

        verify(userService, times(1)).sendOtpForPasswordChange(userSignUpEnquiry, otpRequest.getUsername());
        verify(profileService, times(1)).profileFor(otpRequest.getUsername());
    }

    @Test
    public void ShouldReturnErrorWhenCallingGenerateOtpForInvalidUser() {
        GenerateOtpRequest otpRequest = GenerateOtpRequest.builder().username("username@ncg").build();

        when(profileService.profileFor(otpRequest.getUsername())).thenReturn(Mono.empty());


        webClient.post()
                .uri("/patients/generateotp")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(otpRequest))
                .exchange()
                .expectStatus()
                .isNotFound();

        verify(profileService, times(1)).profileFor(otpRequest.getUsername());
        verifyNoInteractions(userService);
    }

    @Test
    public void verifyOtp(){
        var otpVerification = new OtpVerification(string(), string());
        Token token = new Token(string());

        when(userService.verifyOtp(any())).thenReturn(Mono.just(token));

        webClient.post()
                .uri("/patients/verifyotp")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(otpVerification))
                .exchange().expectStatus().isOk();

        verify(userService, times(1)).verifyOtp(otpVerification);
    }

    @Test
    public void update() {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .password("Test@1324")
                .build();
        Session session = Session.builder()
                .accessToken("accessToken")
                .tokenType("bearer")
                .build();
        var token = string();
        String sessionId = "oldSession";
        String userName = "user@ncg";

        when(signupService.sessionFrom(token)).thenReturn(sessionId);
        when(userService.update(request, sessionId)).thenReturn(Mono.just(session));
        when(signupService.validateToken(token)).thenReturn(Mono.just(true));
        when(signupService.removeOf(sessionId)).thenReturn(Mono.empty());

        webClient
                .put()
                .uri("/patients/profile/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectHeader()
                .contentType(MediaType.APPLICATION_JSON)
                .expectStatus()
                .isOk();

        verify(userService, times(1)).update(request, sessionId);
        verify(signupService, times(1)).sessionFrom(token);
        verify(signupService, times(1)).removeOf(any());
    }

    @Test
    public void shouldReturnErrorWhenPwdIsInvalidWhileUpdating() {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .password("Test123")
                .build();
        var token = string();
        when(signupService.validateToken(token)).thenReturn(Mono.just(true));
        webClient
                .put()
                .uri("/patients/profile/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectHeader()
                .contentType(MediaType.APPLICATION_JSON)
                .expectStatus()
                .isBadRequest();
        verify(userService, times(0)).update(request, "oldSession");
        verify(signupService, times(0)).sessionFrom(token);
        verify(signupService, times(0)).removeOf(any());
    }
}
