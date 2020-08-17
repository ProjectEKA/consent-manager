package in.projecteka.consentmanager.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.common.Authenticator;
import in.projecteka.consentmanager.consent.ConceptValidator;
import in.projecteka.consentmanager.consent.ConsentRequestNotificationListener;
import in.projecteka.consentmanager.consent.HipConsentNotificationListener;
import in.projecteka.consentmanager.consent.HiuConsentNotificationListener;
import in.projecteka.consentmanager.dataflow.DataFlowBroadcastListener;
import in.projecteka.consentmanager.user.model.ChangePinRequest;
import in.projecteka.consentmanager.user.model.CoreSignUpRequest;
import in.projecteka.consentmanager.user.model.DateOfBirth;
import in.projecteka.consentmanager.user.model.Gender;
import in.projecteka.consentmanager.user.model.GenerateOtpRequest;
import in.projecteka.consentmanager.user.model.GenerateOtpResponse;
import in.projecteka.consentmanager.user.model.Identifier;
import in.projecteka.consentmanager.user.model.IdentifierType;
import in.projecteka.consentmanager.user.model.InitiateCmIdRecoveryRequest;
import in.projecteka.consentmanager.user.model.LoginMode;
import in.projecteka.consentmanager.user.model.LoginModeResponse;
import in.projecteka.consentmanager.user.model.OtpAttempt;
import in.projecteka.consentmanager.user.model.OtpMediumType;
import in.projecteka.consentmanager.user.model.OtpVerification;
import in.projecteka.consentmanager.user.model.PatientName;
import in.projecteka.consentmanager.user.model.Profile;
import in.projecteka.consentmanager.user.model.RecoverCmIdResponse;
import in.projecteka.consentmanager.user.model.SendOtpAction;
import in.projecteka.consentmanager.user.model.SignUpSession;
import in.projecteka.consentmanager.user.model.Token;
import in.projecteka.consentmanager.user.model.UpdatePasswordRequest;
import in.projecteka.consentmanager.user.model.UpdateUserRequest;
import in.projecteka.consentmanager.user.model.User;
import in.projecteka.consentmanager.user.model.UserSignUpEnquiry;
import in.projecteka.library.clients.model.ClientError;
import in.projecteka.library.clients.model.Session;
import in.projecteka.library.common.Caller;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static in.projecteka.consentmanager.common.TestBuilders.OBJECT_MAPPER;
import static in.projecteka.consentmanager.user.TestBuilders.coreSignUpRequest;
import static in.projecteka.consentmanager.user.TestBuilders.dateOfBirth;
import static in.projecteka.consentmanager.user.TestBuilders.patientName;
import static in.projecteka.consentmanager.user.TestBuilders.string;
import static java.lang.String.format;
import static org.hamcrest.Matchers.is;
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
class PatientControllerTest {

    @MockBean
    private UserService userService;

    @MockBean
    private BCryptPasswordEncoder encoder;

    @MockBean
    private TransactionPinRepository transactionPinRepository;

    @MockBean
    private LockedUserService lockedUserService;

    @MockBean
    private UserServiceProperties userServiceProperties;

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

    @MockBean
    private Authenticator authenticator;

    @SuppressWarnings("unused")
    @MockBean
    private ConceptValidator conceptValidator;

    @Test
    void createUser() {
        var signUpRequest = coreSignUpRequest()
                .username("username@ncg")
                .name(patientName().first("abc").build())
                .password("@2Abaafasfas")
                .dateOfBirth(dateOfBirth().year(1998).build())
                .build();
        var token = string();
        var sessionId = string();
        when(signupService.sessionFrom(token)).thenReturn(sessionId);
        when(userService.create(any(CoreSignUpRequest.class), eq(sessionId))).thenReturn(Mono.empty());
        when(userService.getUserIdSuffix()).thenReturn("@ncg");
        when(signupService.validateToken(token)).thenReturn(Mono.just(true));
        when(signupService.removeOf(sessionId)).thenReturn(Mono.empty());

        webClient.post()
                .uri("/patients/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .body(BodyInserters.fromValue(signUpRequest))
                .exchange().expectStatus().isCreated();
    }

    @Test
    void returnBadRequestForUserCreation() {
        var signUpRequest = coreSignUpRequest()
                .name(patientName().first("abc").build())
                .dateOfBirth(dateOfBirth().date(LocalDate.now().getDayOfMonth() + 1).build())
                .build();
        var token = string();
        var sessionId = string();
        when(signupService.sessionFrom(token)).thenReturn(sessionId);
        when(userService.create(signUpRequest, sessionId)).thenReturn(Mono.empty());
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
    void generateOtp() throws JsonProcessingException {
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
        var expectedResponseJson = OBJECT_MAPPER.writeValueAsString(expectedResponse);

        when(profileService.profileFor(otpRequest.getUsername())).thenReturn(Mono.just(profile));
        when(userService.sendOtpFor(userSignUpEnquiry, otpRequest.getUsername(), OtpAttempt.Action.OTP_REQUEST_RECOVER_PASSWORD, SendOtpAction.RECOVER_PASSWORD)).thenReturn(Mono.just(signUpSession));

        webClient.post()
                .uri("/patients/generateotp")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(otpRequest))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody()
                .json(expectedResponseJson);

        verify(userService, times(1)).sendOtpFor(userSignUpEnquiry, otpRequest.getUsername(), OtpAttempt.Action.OTP_REQUEST_RECOVER_PASSWORD, SendOtpAction.RECOVER_PASSWORD);
        verify(profileService, times(1)).profileFor(otpRequest.getUsername());
    }

    @Test
    void ShouldReturnErrorWhenCallingGenerateOtpForInvalidUser() {
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
    void verifyOtp() {
        var otpVerification = new OtpVerification(string(), string());
        Token token = new Token(string());

        when(userService.verifyOtpForForgetPassword(any())).thenReturn(Mono.just(token));

        webClient.post()
                .uri("/patients/verifyotp")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(otpVerification))
                .exchange().expectStatus().isOk();

        verify(userService, times(1)).verifyOtpForForgetPassword(otpVerification);
    }

    @Test
    void update() {
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
    void shouldReturnErrorWhenPwdIsInvalidWhileUpdating() {
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

    @Test
    void shouldUpdatePasswordSuccessfully() {
        UpdatePasswordRequest request = UpdatePasswordRequest.builder()
                .oldPassword("Test@1234")
                .newPassword("Test@2020")
                .build();
        String userName = "user@ncg";
        Session expectedSession = Session.builder()
                .accessToken("New access token")
                .tokenType("bearer")
                .build();
        var token = string();

        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller(userName, true)));
        when(userService.updatePassword(request, userName)).thenReturn(Mono.just(expectedSession));
        when(lockedUserService.validateLogin(userName)).thenReturn(Mono.just(userName));
        when(lockedUserService.removeLockedUser(userName)).thenReturn(Mono.empty());

        webClient.put()
                .uri("/patients/profile/update-password")
                .contentType(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isOk();

        verify(userService, times(1)).updatePassword(request, userName);
        verify(authenticator, times(1)).verify(token);
    }

    @Test
    void shouldThrowAnErrorIfUserIsBlockedInUpdatePassword() {
        UpdatePasswordRequest request = UpdatePasswordRequest.builder()
                .oldPassword("Test@1234")
                .newPassword("Test@2020")
                .build();
        String userName = "user@ncg";
        Session expectedSession = Session.builder()
                .accessToken("New access token")
                .tokenType("bearer")
                .build();
        var token = string();

        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller(userName, true)));
        when(lockedUserService.validateLogin(userName)).thenReturn(Mono.error(ClientError.userBlocked()));

        webClient.put()
                .uri("/patients/profile/update-password")
                .contentType(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isUnauthorized();

        verify(authenticator, times(1)).verify(token);
    }

    @Test
    void shouldReturnErrorForInvalidPasswordUpdateRequest() {
        UpdatePasswordRequest request = UpdatePasswordRequest.builder()
                .oldPassword("Test@1234")
                .newPassword("Test")
                .build();
        String userName = "user@ncg";
        var token = string();

        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller(userName, true)));
        when(lockedUserService.validateLogin(userName)).thenReturn(Mono.empty());
        when(lockedUserService.removeLockedUser(userName)).thenReturn(Mono.empty());

        webClient.put()
                .uri("/patients/profile/update-password")
                .contentType(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isBadRequest();

        verify(authenticator, times(1)).verify(token);
        verifyNoInteractions(userService);
    }

    @Test
    void shouldReturnErrorOnUpdatePasswordFails() {
        UpdatePasswordRequest request = UpdatePasswordRequest.builder()
                .oldPassword("Test@1234")
                .newPassword("TestPassword@2020")
                .build();
        String userName = "user@ncg";
        var token = string();

        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller(userName, true)));
        when(userService.updatePassword(request, userName)).thenReturn(Mono.error(ClientError.failedToUpdateUser()));
        when(lockedUserService.validateLogin(userName)).thenReturn(Mono.just(userName));
        when(lockedUserService.removeLockedUser(userName)).thenReturn(Mono.empty());

        webClient.put()
                .uri("/patients/profile/update-password")
                .contentType(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .is5xxServerError();

        verify(userService, times(1)).updatePassword(request, userName);
        verify(authenticator, times(1)).verify(token);
    }

    @Test
    void shouldReturnErrorIfIncorrectOldPasswordGiven() {
        UpdatePasswordRequest request = UpdatePasswordRequest.builder()
                .oldPassword("Test@1234")
                .newPassword("TestPassword@2020")
                .build();
        String userName = "user@ncg";
        var token = string();

        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller(userName, true)));
        when(userService.updatePassword(request, userName)).thenReturn(Mono.error(ClientError.unAuthorizedRequest("Invalid old password")));
        when(lockedUserService.validateLogin(userName)).thenReturn(Mono.just(userName));
        when(lockedUserService.removeLockedUser(userName)).thenReturn(Mono.empty());

        webClient.put()
                .uri("/patients/profile/update-password")
                .contentType(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isUnauthorized();

        verify(userService, times(1)).updatePassword(request, userName);
        verify(authenticator, times(1)).verify(token);
    }

    @Test
    void fetchLoginMode() {
        LoginModeResponse loginModeResponse = LoginModeResponse.builder()
                .loginMode(LoginMode.CREDENTIAL)
                .build();
        String userName = "user@ncg";

        when(userService.getLoginMode(userName)).thenReturn(Mono.just(loginModeResponse));

        webClient
                .get()
                .uri(format("/patients/profile/loginmode?userName=%s", userName))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(LoginModeResponse.class)
                .value(LoginModeResponse::getLoginMode, is(LoginMode.CREDENTIAL));

        verify(userService, times(1)).getLoginMode(userName);
    }

    @Test
    void generateOtpOnGettingSinglePatientRecordForInitiateRecoverCmId() throws JsonProcessingException {
        PatientName name = patientName().first("abc").build();
        Gender gender = Gender.F;
        DateOfBirth dateOfBirth = dateOfBirth().year(1998).build();
        String verifiedIdentifierValue = "+91-9999999999";
        String unverifiedIdentifierValue = "P1234ABCD";
        ArrayList<Identifier> verifiedIdentifiers = new ArrayList<>(Collections.singletonList(new Identifier(IdentifierType.MOBILE, verifiedIdentifierValue)));
        ArrayList<Identifier> unverifiedIdentifiers = new ArrayList<>(Collections.singletonList(new Identifier(IdentifierType.ABPMJAYID, unverifiedIdentifierValue)));
        String cmId = "abc@ncg";
        InitiateCmIdRecoveryRequest request = new InitiateCmIdRecoveryRequest(name, gender, dateOfBirth, verifiedIdentifiers, unverifiedIdentifiers);
        JsonArray unverifiedIdentifiersResponse = new JsonArray().add(new JsonObject().put("type", "ABPMJAYID").put("value", unverifiedIdentifierValue));
        User user = User.builder().identifier(cmId).phone(verifiedIdentifierValue).name(patientName().first("abc").
                build()).dateOfBirth(dateOfBirth()
                .year(1998).build()).
                unverifiedIdentifiers(unverifiedIdentifiersResponse).
                build();

        UserSignUpEnquiry userSignUpEnquiry = UserSignUpEnquiry.builder()
                .identifierType(IdentifierType.MOBILE.toString())
                .identifier(verifiedIdentifierValue)
                .build();
        SignUpSession signUpSession = new SignUpSession("sessionId");
        GenerateOtpResponse expectedResponse = GenerateOtpResponse.builder()
                .otpMedium(OtpMediumType.MOBILE.toString())
                .otpMediumValue("******9999")
                .sessionId(signUpSession.getSessionId())
                .build();
        var expectedResponseJson = OBJECT_MAPPER.writeValueAsString(expectedResponse);

        when(userService.getPatientByDetails(any())).thenReturn(Mono.just(user));
        when(userService.sendOtpFor(any(), any(), any(), any())).thenReturn(Mono.just(signUpSession));

        webClient.post()
                .uri("/patients/profile/recovery-init")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(GenerateOtpResponse.class)
                .value(GenerateOtpResponse::getOtpMedium, is(OtpMediumType.MOBILE.toString()));

        verify(userService, times(1)).sendOtpFor(userSignUpEnquiry, user.getIdentifier(), OtpAttempt.Action.OTP_REQUEST_RECOVER_CM_ID, SendOtpAction.RECOVER_CM_ID);
    }

    @Test
    void shouldThrowAnErrorWhenNoOrMultipleMatchingPatientRecordFoundForInitiateRecoverCmId() throws JsonProcessingException {
        PatientName name = patientName().first("abc").build();
        Gender gender = Gender.F;
        DateOfBirth dateOfBirth = dateOfBirth().year(1998).build();
        String verifiedIdentifierValue = "+91-9999999999";
        String unverifiedIdentifierValue = "P1234ABCD";
        ArrayList<Identifier> verifiedIdentifiers = new ArrayList<>(Collections.singletonList(new Identifier(IdentifierType.MOBILE, verifiedIdentifierValue)));
        ArrayList<Identifier> unverifiedIdentifiers = new ArrayList<>(Collections.singletonList(new Identifier(IdentifierType.ABPMJAYID, unverifiedIdentifierValue)));
        String cmId = "abc@ncg";
        InitiateCmIdRecoveryRequest request = new InitiateCmIdRecoveryRequest(name, gender, dateOfBirth, verifiedIdentifiers, unverifiedIdentifiers);

        when(userService.getPatientByDetails(any())).thenReturn(Mono.empty());

        webClient.post()
                .uri("/patients/profile/recovery-init")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void shouldThrowAnErrorWhenMendatoryFieldsAreNotProvidedInRequestBodyForInitiateRecoverCMId() {
        PatientName name = patientName().first("abc").build();
        Gender gender = null;
        DateOfBirth dateOfBirth = dateOfBirth().year(1998).build();
        String verifiedIdentifierValue = "+91-9999999999";
        String unverifiedIdentifierValue = "P1234ABCD";
        ArrayList<Identifier> verifiedIdentifiers = new ArrayList<>(Collections.singletonList(new Identifier(IdentifierType.MOBILE, verifiedIdentifierValue)));
        ArrayList<Identifier> unverifiedIdentifiers = new ArrayList<>(Collections.singletonList(new Identifier(IdentifierType.ABPMJAYID, unverifiedIdentifierValue)));
        String cmId = "abc@ncg";
        InitiateCmIdRecoveryRequest request = new InitiateCmIdRecoveryRequest(name, gender, dateOfBirth, verifiedIdentifiers, unverifiedIdentifiers);

        webClient.post()
                .uri("/patients/profile/recovery-init")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    void shouldThrowAnErrorWhenIdentifiersAreMappedIncorrectlyInRequestBodyForInitiateRecoverCMId() {
        PatientName name = patientName().first("abc").build();
        Gender gender = Gender.F;
        DateOfBirth dateOfBirth = dateOfBirth().year(1998).build();
        String verifiedIdentifierValue = "+91-9999999999";
        String unverifiedIdentifierValue = "P1234ABCD";
        ArrayList<Identifier> verifiedIdentifiers = new ArrayList<>(List.of(new Identifier(IdentifierType.MOBILE, verifiedIdentifierValue), new Identifier(IdentifierType.ABPMJAYID, unverifiedIdentifierValue)));
        ArrayList<Identifier> unverifiedIdentifiers = new ArrayList<>(Collections.singletonList(new Identifier(IdentifierType.ABPMJAYID, unverifiedIdentifierValue)));
        String cmId = "abc@ncg";
        InitiateCmIdRecoveryRequest request = new InitiateCmIdRecoveryRequest(name, gender, dateOfBirth, verifiedIdentifiers, unverifiedIdentifiers);

        webClient.post()
                .uri("/patients/profile/recovery-init")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    void shouldThrowAnErrorIfSignUpSessionIsNullForInitiateRecoverCmId() throws JsonProcessingException {
        PatientName name = patientName().first("abc").build();
        Gender gender = Gender.F;
        DateOfBirth dateOfBirth = dateOfBirth().year(1998).build();
        String verifiedIdentifierValue = "+91-9999999999";
        String unverifiedIdentifierValue = "P1234ABCD";
        ArrayList<Identifier> verifiedIdentifiers = new ArrayList<>(Collections.singletonList(new Identifier(IdentifierType.MOBILE, verifiedIdentifierValue)));
        ArrayList<Identifier> unverifiedIdentifiers = new ArrayList<>(Collections.singletonList(new Identifier(IdentifierType.ABPMJAYID, unverifiedIdentifierValue)));
        String cmId = "abc@ncg";
        InitiateCmIdRecoveryRequest request = new InitiateCmIdRecoveryRequest(name, gender, dateOfBirth, verifiedIdentifiers, unverifiedIdentifiers);
        JsonArray unverifiedIdentifiersResponse = new JsonArray().add(new JsonObject().put("type", "ABPMJAYID").put("value", unverifiedIdentifierValue));
        User user = User.builder().identifier(cmId).phone(verifiedIdentifierValue)
                .name(patientName().first("abc").build())
                .dateOfBirth(dateOfBirth().year(1998).build())
                .unverifiedIdentifiers(unverifiedIdentifiersResponse)
                .build();

        UserSignUpEnquiry userSignUpEnquiry = UserSignUpEnquiry.builder()
                .identifierType(IdentifierType.MOBILE.toString())
                .identifier(verifiedIdentifierValue)
                .build();
        SignUpSession signUpSession = new SignUpSession(null);
        GenerateOtpResponse expectedResponse = GenerateOtpResponse.builder()
                .otpMedium(OtpMediumType.MOBILE.toString())
                .otpMediumValue("******9999")
                .sessionId(signUpSession.getSessionId())
                .build();
        var expectedResponseJson = OBJECT_MAPPER.writeValueAsString(expectedResponse);

        when(userService.getPatientByDetails(any())).thenReturn(Mono.just(user));

        webClient.post()
                .uri("/patients/profile/recovery-init")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus()
                .is5xxServerError();

        verify(userService, times(1)).sendOtpFor(userSignUpEnquiry, user.getIdentifier(), OtpAttempt.Action.OTP_REQUEST_RECOVER_CM_ID, SendOtpAction.RECOVER_CM_ID);
    }

    @Test
    void verifyOtpForRecoveringCmId() {
        var otpVerification = new OtpVerification(string(), string());
        String cmId = "testUser@ncg";

        when(userService.verifyOtpForRecoverCmId(any())).thenReturn(Mono.just(RecoverCmIdResponse.builder().cmId(cmId).build()));

        webClient.post()
                .uri("/patients/profile/recovery-confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(otpVerification))
                .exchange().expectStatus().isOk();

        verify(userService, times(1)).verifyOtpForRecoverCmId(otpVerification);
    }

    @Test
    void shouldGenerateOTPForForgetPin() throws JsonProcessingException {
        var captor = ArgumentCaptor.forClass(UserSignUpEnquiry.class);
        var cmId = "abc@ncg";
        var token = string();
        SignUpSession session = new SignUpSession(UUID.randomUUID().toString());

        User user = User.builder().identifier(cmId).phone("+91-9999999999")
                .name(patientName().first("abc").build())
                .dateOfBirth(dateOfBirth().year(1998).build())
                .build();

        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller(cmId, true)));
        when(userService.userWith(cmId)).thenReturn(Mono.just(user));
        when(userService.sendOtpFor(captor.capture(),
                eq(cmId),
                eq(OtpAttempt.Action.OTP_REQUEST_FORGOT_CONSENT_PIN),
                eq(SendOtpAction.FORGOT_CONSENT_PIN))).thenReturn(Mono.just(session));
        when(userService.getExpiryInMinutes()).thenReturn(1);

        var expectedResponse = GenerateOtpResponse.builder()
                .expiryInMinutes(1)
                .otpMedium("MOBILE")
                .otpMediumValue("+**-******9999")
                .sessionId(session.getSessionId())
                .build();

        webClient.post()
                .uri(Constants.BASE_PATH_PATIENTS_APIS + Constants.APP_PATH_FORGET_PIN_GENERATE_OTP)
                .header(AUTHORIZATION, token)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody()
                .json(new ObjectMapper()
                        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                        .writeValueAsString(expectedResponse));
    }

    @Test
    void shouldValidateOTPForForgetPin() throws JsonProcessingException {
        var cmId = "abc@ncg";
        var token = string();
        User user = User.builder().identifier(cmId).phone("+91-9999999999")
                .name(patientName().first("abc").build())
                .dateOfBirth(dateOfBirth().year(1998).build())
                .build();
        OtpVerification otpVerification = new OtpVerification(UUID.randomUUID().toString(),user.getPhone());
        var tempToken = new Token("temp-token");

        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller(cmId, true)));
        when(userService.verifyOtpForForgotConsentPin(otpVerification)).thenReturn(Mono.just(tempToken));

        webClient.post()
                .uri(Constants.BASE_PATH_PATIENTS_APIS + Constants.APP_PATH_FORGET_PIN_VALIDATE_OTP)
                .header(AUTHORIZATION, token)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(otpVerification))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .json(new ObjectMapper()
                        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                        .writeValueAsString(tempToken));
    }

    @Test
    void shouldResetPinForForgetPin(){
        var token = string();
        var cmId = "cm-id";
        var sessionId = "session-id";
        var pin = "4444";
        var encodedPin = "xyz@123";

        when(userServiceProperties.getTransactionPinDigitSize()).thenReturn(4);
        when(signupService.validateToken(token)).thenReturn(Mono.just(true));
        when(signupService.sessionFrom(token)).thenReturn(sessionId);
        when(signupService.getUserName(sessionId)).thenReturn(Mono.just(cmId));
        var changePinRequest = new ChangePinRequest(pin);

        when(encoder.encode(pin)).thenReturn(encodedPin);
        when(transactionPinRepository.changeTransactionPin(cmId,encodedPin)).thenReturn(Mono.empty());
        when(signupService.removeOf(sessionId)).thenReturn(Mono.empty());

        webClient.put()
                .uri(Constants.BASE_PATH_PATIENTS_APIS + Constants.APP_PATH_RESET_PIN)
                .header(AUTHORIZATION, token)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(changePinRequest))
                .exchange()
                .expectStatus()
                .isNoContent();
    }

}
