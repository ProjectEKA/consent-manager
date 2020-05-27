package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.NullableConverter;
import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.IdentityServiceClient;
import in.projecteka.consentmanager.clients.OtpServiceClient;
import in.projecteka.consentmanager.clients.model.KeyCloakUserCredentialRepresentation;
import in.projecteka.consentmanager.clients.model.KeyCloakUserRepresentation;
import in.projecteka.consentmanager.clients.model.OtpRequest;
import in.projecteka.consentmanager.clients.model.Session;
import in.projecteka.consentmanager.clients.properties.OtpServiceProperties;
import in.projecteka.consentmanager.common.DbOperationError;
import in.projecteka.consentmanager.user.exception.InvalidRequestException;
import in.projecteka.consentmanager.user.model.Gender;
import in.projecteka.consentmanager.user.model.Identifier;
import in.projecteka.consentmanager.user.model.IdentifierType;
import in.projecteka.consentmanager.user.model.LoginMode;
import in.projecteka.consentmanager.user.model.LoginModeResponse;
import in.projecteka.consentmanager.user.model.OtpVerification;
import in.projecteka.consentmanager.user.model.RecoverCmIdRequest;
import in.projecteka.consentmanager.user.model.User;
import in.projecteka.consentmanager.user.model.SignUpSession;
import in.projecteka.consentmanager.user.model.Token;
import in.projecteka.consentmanager.user.model.UpdatePasswordRequest;
import in.projecteka.consentmanager.user.model.UpdateUserRequest;
import in.projecteka.consentmanager.user.model.UserSignUpEnquiry;
import in.projecteka.consentmanager.user.model.OtpAttempt;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static in.projecteka.consentmanager.user.TestBuilders.coreSignUpRequest;
import static in.projecteka.consentmanager.user.TestBuilders.session;
import static in.projecteka.consentmanager.user.TestBuilders.string;
import static in.projecteka.consentmanager.user.TestBuilders.user;
import static in.projecteka.consentmanager.user.TestBuilders.userSignUpEnquiry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserServiceTest {

    @Captor
    private ArgumentCaptor<OtpRequest> otpRequestArgumentCaptor;

    @Captor
    private ArgumentCaptor<String> sessionCaptor;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OtpServiceClient otpServiceClient;

    @Mock
    private SignUpService signupService;

    @Mock
    private OtpAttemptService otpAttemptService;

    @Mock
    private LockedUserService lockedUserService;

    @Mock
    private IdentityServiceClient identityServiceClient;

    @Mock
    private TokenService tokenService;

    @Mock
    private UserServiceProperties properties;

    @Captor
    private ArgumentCaptor<ClientRequest> captor;

    @Mock
    private ExchangeFunction exchangeFunction;

    @Mock
    private Logger logger;

    private UserService userService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        var otpServiceProperties = new OtpServiceProperties("", Collections.singletonList("MOBILE"), 5);
        userService = new UserService(
                userRepository,
                otpServiceProperties,
                otpServiceClient,
                signupService,
                identityServiceClient,
                tokenService,
                properties,
                otpAttemptService,
                lockedUserService);
    }

    @Test
    public void shouldReturnTemporarySessionReceivedFromClient() {
        var userSignUpEnquiry = new UserSignUpEnquiry("MOBILE", "+91-9788888");
        var sessionId = string();
        var signUpSession = new SignUpSession(sessionId);
        when(otpServiceClient.send(otpRequestArgumentCaptor.capture())).thenReturn(Mono.empty());
        when(signupService.cacheAndSendSession(sessionCaptor.capture(), eq("+91-9788888")))
                .thenReturn(Mono.just(signUpSession));
        when(otpAttemptService.validateOTPRequest(userSignUpEnquiry.getIdentifierType(), userSignUpEnquiry.getIdentifier(), OtpAttempt.Action.OTP_REQUEST_REGISTRATION)).thenReturn(Mono.empty());

        Mono<SignUpSession> signUp = userService.sendOtp(userSignUpEnquiry);

        assertThat(otpRequestArgumentCaptor.getValue().getSessionId()).isEqualTo(sessionCaptor.getValue());
        StepVerifier.create(signUp)
                .assertNext(session -> assertThat(session).isEqualTo(signUpSession))
                .verifyComplete();
    }

    @Test
    public void shouldThrowInvalidRequestExceptionForInvalidDeviceType() {
        Assertions.assertThrows(InvalidRequestException.class, () -> userService.sendOtp(userSignUpEnquiry().build()));
    }

    @Test
    public void shouldReturnTokenReceivedFromClient() {
        var sessionId = string();
        var otp = string();
        var token = string();
        var mobileNumber = "+91-8888888888";

        ArgumentCaptor<OtpAttempt> argument = ArgumentCaptor.forClass(OtpAttempt.class);
        OtpVerification otpVerification = new OtpVerification(sessionId, otp);
        when(otpServiceClient.verify(eq(sessionId), eq(otp))).thenReturn(Mono.empty());
        when(signupService.generateToken(sessionId))
                .thenReturn(Mono.just(new Token(token)));
        when(signupService.getMobileNumber(eq(sessionId))).thenReturn(Mono.just(mobileNumber));
        when(otpAttemptService.validateOTPSubmission(argument.capture())).thenReturn(Mono.empty());
        when(otpAttemptService.removeMatchingAttempts(argument.capture())).thenReturn(Mono.empty());
        StepVerifier.create(userService.verifyOtpForRegistration(otpVerification))
                .assertNext(response -> assertThat(response.getTemporaryToken()).isEqualTo(token))
                .verifyComplete();

        var capturedAttempts = argument.getAllValues();
        var validateOTPSubmissionArgument = capturedAttempts.get(0);
        assertEquals(sessionId, validateOTPSubmissionArgument.getSessionId());
        assertEquals("MOBILE", validateOTPSubmissionArgument.getIdentifierType());
        assertEquals(mobileNumber, validateOTPSubmissionArgument.getIdentifierValue());
        assertEquals(OtpAttempt.Action.OTP_SUBMIT_REGISTRATION, validateOTPSubmissionArgument.getAction());

        var removeMatchingAttemptsArgument = capturedAttempts.get(1);
        assertEquals(sessionId, removeMatchingAttemptsArgument.getSessionId());
        assertEquals("MOBILE", removeMatchingAttemptsArgument.getIdentifierType());
        assertEquals(mobileNumber, removeMatchingAttemptsArgument.getIdentifierValue());
        assertEquals(OtpAttempt.Action.OTP_SUBMIT_REGISTRATION, removeMatchingAttemptsArgument.getAction());
    }

    @ParameterizedTest(name = "Invalid values")
    @CsvSource({
            ",",
            "empty",
            "null"
    })
    public void shouldThrowInvalidRequestExceptionForInvalidOtpValue(
            @ConvertWith(NullableConverter.class) String value) {
        OtpVerification otpVerification = new OtpVerification(string(), value);
        Assertions.assertThrows(InvalidRequestException.class, () -> userService.verifyOtpForRegistration(otpVerification));
    }

    @ParameterizedTest(name = "Invalid session id")
    @CsvSource({
            ",",
            "empty",
            "null"
    })
    public void shouldThrowInvalidRequestExceptionForInvalidOtpSessionId(
            @ConvertWith(NullableConverter.class) String sessionId) {
        OtpVerification otpVerification = new OtpVerification(sessionId, string());
        Assertions.assertThrows(InvalidRequestException.class, () -> userService.verifyOtpForRegistration(otpVerification));
    }

    @Test
    public void verifyOtp() {
        var sessionId = string();
        var otp = string();
        var token = string();
        var user = new EasyRandom().nextObject(User.class);
        OtpVerification otpVerification = new OtpVerification(sessionId, otp);
        when(otpServiceClient.verify(eq(sessionId), eq(otp))).thenReturn(Mono.empty());
        when(signupService.generateToken(new HashMap<>(),sessionId))
                .thenReturn(Mono.just(new Token(token)));
        when(signupService.getUserName(eq(sessionId))).thenReturn(Mono.just(user.getIdentifier()));
        when(userRepository.userWith(eq(user.getIdentifier()))).thenReturn(Mono.just(user));
        when(lockedUserService.validateLogin(eq(user.getIdentifier()))).thenReturn(Mono.empty());
        when(lockedUserService.removeLockedUser(eq(user.getIdentifier()))).thenReturn(Mono.empty());
        StepVerifier.create(userService.verifyOtpForForgetPassword(otpVerification))
                .assertNext(response -> assertThat(response.getTemporaryToken()).isEqualTo(token))
                .verifyComplete();
    }

    @ParameterizedTest(name = "Invalid values")
    @CsvSource({
            ",",
            "empty",
            "null"
    })
    public void shouldThrowErrorForInvalidOtpValue(
            @ConvertWith(NullableConverter.class) String value) {
        OtpVerification otpVerification = new OtpVerification(string(), value);
        Assertions.assertThrows(InvalidRequestException.class, () -> userService.verifyOtpForForgetPassword(otpVerification));
    }

    @ParameterizedTest(name = "Invalid session id")
    @CsvSource({
            ",",
            "empty",
            "null"
    })
    public void shouldThrowErrorForInvalidOtpSessionId(
            @ConvertWith(NullableConverter.class) String sessionId) {
        OtpVerification otpVerification = new OtpVerification(sessionId, string());
        Assertions.assertThrows(InvalidRequestException.class, () -> userService.verifyOtpForForgetPassword(otpVerification));
    }

    @Test
    public void shouldCreateUser() {
        var signUpRequest = coreSignUpRequest().yearOfBirth(LocalDate.now().getYear()).build();
        var userToken = session().build();
        var sessionId = string();
        var mobileNumber = string();
        when(tokenService.tokenForAdmin()).thenReturn(Mono.just(new Session()));
        when(signupService.getMobileNumber(sessionId)).thenReturn(Mono.just(mobileNumber));
        when(identityServiceClient.createUser(any(), any())).thenReturn(Mono.empty());
        when(userRepository.save(any())).thenReturn(Mono.empty());
        when(userRepository.userWith(signUpRequest.getUsername())).thenReturn(Mono.empty());
        when(tokenService.tokenForUser(any(), any())).thenReturn(Mono.just(userToken));

        StepVerifier.create(userService.create(signUpRequest, sessionId))
                .assertNext(response -> assertThat(response.getAccessToken()).isEqualTo(userToken.getAccessToken()))
                .verifyComplete();
    }

    @Test
    public void shouldReturnUserAlreadyExistsError() {
        var signUpRequest = coreSignUpRequest().yearOfBirth(LocalDate.MIN.getYear()).build();
        var sessionId = string();
        var user = user().identifier(signUpRequest.getUsername()).build();
        when(signupService.getMobileNumber(sessionId)).thenReturn(Mono.just(string()));
        when(userRepository.userWith(signUpRequest.getUsername())).thenReturn(Mono.just(user));
        when(userRepository.save(any())).thenReturn(Mono.empty());

        Mono<Session> publisher = userService.create(signUpRequest, sessionId);
        StepVerifier.create(publisher)
                .verifyErrorSatisfies(error -> assertThat(error)
                        .asInstanceOf(InstanceOfAssertFactories.type(ClientError.class))
                        .isEqualToComparingFieldByField(ClientError.userAlreadyExists(signUpRequest.getUsername())));
    }

    @Test
    public void shouldCreateUserWhenYOBIsNull() {
        var signUpRequest = coreSignUpRequest().name("apoorva g a").yearOfBirth(null).build();
        var userToken = session().build();
        var sessionId = string();
        var mobileNumber = string();
        when(tokenService.tokenForAdmin()).thenReturn(Mono.just(new Session()));
        when(signupService.getMobileNumber(sessionId)).thenReturn(Mono.just(mobileNumber));
        when(identityServiceClient.createUser(any(), any())).thenReturn(Mono.empty());
        when(userRepository.userWith(signUpRequest.getUsername())).thenReturn(Mono.empty());
        when(userRepository.save(any())).thenReturn(Mono.empty());
        when(tokenService.tokenForUser(any(), any())).thenReturn(Mono.just(userToken));

        StepVerifier.create(userService.create(signUpRequest, sessionId))
                .assertNext(response -> assertThat(response.getAccessToken()).isEqualTo(userToken.getAccessToken()))
                .verifyComplete();
    }

    @Test
    public void shouldNotCreateUserWhenIDPClientFails() {
        var signUpRequest = coreSignUpRequest()
                .yearOfBirth(LocalDate.MIN.getYear())
                .build();
        var mobileNumber = string();
        var user = User.from(signUpRequest, mobileNumber);
        var identifier = user.getIdentifier();
        var sessionId = string();
        var tokenForAdmin = session().build();

        when(signupService.getMobileNumber(sessionId)).thenReturn(Mono.just(mobileNumber));
        when(userRepository.userWith(signUpRequest.getUsername())).thenReturn(Mono.empty());
        when(userRepository.save(any())).thenReturn(Mono.empty());
        when(tokenService.tokenForUser(any(), any())).thenReturn(Mono.empty());
        when(tokenService.tokenForAdmin()).thenReturn(Mono.just(tokenForAdmin));
        when(identityServiceClient.createUser(any(), any())).thenReturn(Mono.error(ClientError.networkServiceCallFailed()));
        when(userRepository.delete(identifier)).thenReturn(Mono.empty());

        Mono<Session> publisher = userService.create(signUpRequest, sessionId);

        StepVerifier.create(publisher).verifyComplete();
        verify(userRepository, times(1)).delete(identifier);
    }

    @Test
    void shouldNotCreateUserWhenPersistingToDbFails() {
        var signUpRequest = coreSignUpRequest()
                .yearOfBirth(LocalDate.MIN.getYear())
                .build();
        var mobileNumber = string();
        var user = User.from(signUpRequest, mobileNumber);
        var identifier = user.getIdentifier();
        var sessionId = string();
        var tokenForAdmin = session().build();

        when(signupService.getMobileNumber(sessionId)).thenReturn(Mono.just(mobileNumber));
        when(userRepository.userWith(signUpRequest.getUsername())).thenReturn(Mono.empty());
        when(userRepository.save(any())).thenReturn(Mono.error(new DbOperationError()));
        when(tokenService.tokenForUser(any(), any())).thenReturn(Mono.empty());
        when(tokenService.tokenForAdmin()).thenReturn(Mono.just(tokenForAdmin));
        when(identityServiceClient.createUser(any(), any())).thenReturn(Mono.empty());
        when(userRepository.delete(identifier)).thenReturn(Mono.empty());

        Mono<Session> publisher = userService.create(signUpRequest, sessionId);

        StepVerifier.create(publisher)
                .verifyErrorSatisfies(error -> assertThat(error)
                        .asInstanceOf(InstanceOfAssertFactories.type(DbOperationError.class))
                        .isEqualToComparingFieldByField(new DbOperationError()));
    }

    @Test
    public void updateUserDetails() {
        String userName = "user@ncg";
        UpdateUserRequest request = UpdateUserRequest.builder()
                .password("Test@3142")
                .build();
        var sessionId = string();
        Session tokenForAdmin = session().build();
        String token = String.format("Bearer %s", tokenForAdmin.getAccessToken());
        KeyCloakUserRepresentation userRepresentation = KeyCloakUserRepresentation.builder()
                .id("keycloakuserid")
                .build();
        var newSession = session().build();

        when(signupService.getUserName(sessionId)).thenReturn(Mono.just(userName));
        when(tokenService.tokenForAdmin()).thenReturn(Mono.just(tokenForAdmin));
        when(identityServiceClient.getUser(userName, token)).thenReturn(Flux.just(userRepresentation));
        when(identityServiceClient.updateUser(tokenForAdmin, userRepresentation.getId(), request.getPassword())).thenReturn(Mono.empty());
        when(tokenService.tokenForUser(userName, request.getPassword())).thenReturn(Mono.just(newSession));

        Mono<Session> updatedSession = userService.update(request, sessionId);

        StepVerifier.create(updatedSession)
                .assertNext(response -> assertThat(response.getAccessToken()).isEqualTo(newSession.getAccessToken()))
                .verifyComplete();
    }

    @Test
    public void updateUserDetailsFailsForInvalidUserName() {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .password("Test@3142")
                .build();
        var sessionId = string();

        when(signupService.getUserName(sessionId)).thenReturn(Mono.empty());

        Mono<Session> updatedSession = userService.update(request, sessionId);

        StepVerifier.create(updatedSession)
                .verifyErrorMatches(throwable -> throwable instanceof InvalidRequestException &&
                        ((InvalidRequestException) throwable).getMessage().equals("user not verified"));
    }

    @Test
    public void updateUserDetailsFailsWhenTokenForAdminFails() {
        String userName = "user@ncg";
        UpdateUserRequest request = UpdateUserRequest.builder()
                .password("Test@3142")
                .build();
        var sessionId = string();
        var newSession = session().build();

        when(signupService.getUserName(sessionId)).thenReturn(Mono.just(userName));
        when(tokenService.tokenForAdmin()).thenReturn(Mono.error(ClientError.failedToUpdateUser()));
        when(tokenService.tokenForUser(userName, request.getPassword())).thenReturn(Mono.just(newSession));

        Mono<Session> updatedSession = userService.update(request, sessionId);

        StepVerifier.create(updatedSession)
                .verifyErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().value() == 500);

        verifyNoInteractions(identityServiceClient);
    }

    @Test
    public void shouldUpdatePasswordSuccessfully() {
        String userName = "testUser@ncg";
        UpdatePasswordRequest request = UpdatePasswordRequest.builder()
                .newPassword("TestPassword@123")
                .oldPassword("password@09")
                .build();
        Session oldSession = session().build();
        Session newSession = session().build();
        Session tokenForAdmin = session().build();
        String token = String.format("Bearer %s", tokenForAdmin.getAccessToken());
        KeyCloakUserRepresentation userRepresentation = KeyCloakUserRepresentation.builder()
                .id("keycloakuserid")
                .build();
        when(tokenService.tokenForUser(userName, request.getOldPassword())).thenReturn(Mono.just(oldSession));
        when(tokenService.tokenForAdmin()).thenReturn(Mono.just(tokenForAdmin));
        when(identityServiceClient.getUser(userName, token)).thenReturn(Flux.just(userRepresentation));
        when(identityServiceClient.updateUser(tokenForAdmin, userRepresentation.getId(), request.getNewPassword())).thenReturn(Mono.empty());
        when(tokenService.tokenForUser(userName, request.getNewPassword())).thenReturn(Mono.just(newSession));

        Mono<Session> updatedPasswordSession = userService.updatePassword(request, userName);

        StepVerifier.create(updatedPasswordSession)
                .assertNext(response -> assertThat(response.getAccessToken()).isEqualTo(newSession.getAccessToken()))
                        .verifyComplete();

        verify(tokenService, times(1)).tokenForAdmin();
        verify(tokenService, times(1)).tokenForUser(userName, request.getOldPassword());
        verify(identityServiceClient, times(1)).getUser(userName, token);
        verify(identityServiceClient, times(1)).updateUser(tokenForAdmin, userRepresentation.getId(), request.getNewPassword());
        verify(tokenService, times(1)).tokenForUser(userName, request.getNewPassword());
    }

    @Test
    public void shouldReturnUnauthorizedErrorForInvalidOldPassword() {
        String userName = "TestUser@ncg";
        UpdatePasswordRequest request = UpdatePasswordRequest.builder()
                .oldPassword("Test@123")
                .newPassword("TestPW@1234")
                .build();

        when(tokenService.tokenForUser(userName, request.getOldPassword())).thenReturn(Mono.error(ClientError.unAuthorizedRequest("Invalid Old Password")));

        Mono<Session> updatedPasswordSession = userService.updatePassword(request, userName);

        StepVerifier.create(updatedPasswordSession)
                .verifyErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().value() == 401);

        verify(tokenService, times(1)).tokenForUser(userName, request.getOldPassword());
        verifyNoInteractions(identityServiceClient);
    }

    @Test
    public void shouldReturnErrorWhenUpdateUserWithNewPasswordFails() {
        String userName = "user@ncg";
        UpdatePasswordRequest request = UpdatePasswordRequest.builder()
                .oldPassword("Test@3142")
                .newPassword("Test@1234")
                .build();
        var oldSession = session().build();
        var newSession = session().build();
        Session tokenForAdmin = session().build();
        String token = String.format("Bearer %s", tokenForAdmin.getAccessToken());
        KeyCloakUserRepresentation userRepresentation = KeyCloakUserRepresentation.builder()
                .id("keycloakuserid")
                .build();

        when(tokenService.tokenForUser(userName, request.getOldPassword())).thenReturn(Mono.just(oldSession));
        when(tokenService.tokenForAdmin()).thenReturn(Mono.just(tokenForAdmin));
        when(identityServiceClient.getUser(userName, token)).thenReturn(Flux.just(userRepresentation));
        when(identityServiceClient.updateUser(tokenForAdmin, userRepresentation.getId(), request.getNewPassword())).thenReturn(Mono.error(ClientError.failedToUpdateUser()));
        when(tokenService.tokenForUser(userName, request.getNewPassword())).thenReturn(Mono.just(newSession));

        Mono<Session> updatedPasswordSession = userService.updatePassword(request, userName);

        StepVerifier.create(updatedPasswordSession)
                .verifyErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().value() == 500);

        verify(tokenService, times(1)).tokenForAdmin();
        verify(tokenService, times(1)).tokenForUser(userName, request.getOldPassword());
        verify(identityServiceClient, times(1)).getUser(userName, token);
        verify(identityServiceClient, times(1)).updateUser(tokenForAdmin, userRepresentation.getId(), request.getNewPassword());
        verify(tokenService, times(1)).tokenForUser(userName, request.getNewPassword());
    }
    @Test
    public void getLoginMode() {
        String userName = "user@ncg";
        Session tokenForAdmin = session().build();
        String token = String.format("Bearer %s", tokenForAdmin.getAccessToken());
        KeyCloakUserRepresentation userRepresentation = KeyCloakUserRepresentation.builder()
                .id("keycloakuserid")
                .build();
        Flux<KeyCloakUserCredentialRepresentation> userCreds = Flux.just(KeyCloakUserCredentialRepresentation
                .builder()
                .id("credid")
                .type("password")
                .build());

        when(tokenService.tokenForAdmin()).thenReturn(Mono.just(tokenForAdmin));
        when(identityServiceClient.getUser(userName, token)).thenReturn(Flux.just(userRepresentation));
        when(identityServiceClient.getCredentials(userRepresentation.getId(), token)).thenReturn(userCreds);

        Mono<LoginModeResponse> loginModeResponse = userService.getLoginMode(userName);

        StepVerifier.create(loginModeResponse)
                .assertNext(response -> assertThat(response.getLoginMode()).isEqualTo(LoginMode.CREDENTIAL))
                .verifyComplete();
        verify(tokenService, times(1)).tokenForAdmin();
        verify(identityServiceClient, times(1)).getUser(userName, token);
        verify(identityServiceClient, times(1)).getCredentials(userRepresentation.getId(), token);
    }

    @Test
    public void getLoginModeAsOTPWhenPasswordNotSet() {
        String userName = "user@ncg";
        Session tokenForAdmin = session().build();
        String token = String.format("Bearer %s", tokenForAdmin.getAccessToken());
        KeyCloakUserRepresentation userRepresentation = KeyCloakUserRepresentation.builder()
                .id("keycloakuserid")
                .build();

        when(tokenService.tokenForAdmin()).thenReturn(Mono.just(tokenForAdmin));
        when(identityServiceClient.getUser(userName, token)).thenReturn(Flux.just(userRepresentation));
        when(identityServiceClient.getCredentials(userRepresentation.getId(), token)).thenReturn(Flux.empty());

        Mono<LoginModeResponse> loginModeResponse = userService.getLoginMode(userName);

        StepVerifier.create(loginModeResponse)
                .assertNext(response -> assertThat(response.getLoginMode()).isEqualTo(LoginMode.OTP))
                .verifyComplete();
        verify(tokenService, times(1)).tokenForAdmin();
        verify(identityServiceClient, times(1)).getUser(userName, token);
        verify(identityServiceClient, times(1)).getCredentials(userRepresentation.getId(), token);
    }

    @Test
    public void getLoginModeReturnsErrorForNonExistentUser() {
        String userName = "user@ncg";
        Session tokenForAdmin = session().build();
        String token = String.format("Bearer %s", tokenForAdmin.getAccessToken());
        KeyCloakUserRepresentation userRepresentation = KeyCloakUserRepresentation.builder().build();

        when(tokenService.tokenForAdmin()).thenReturn(Mono.just(tokenForAdmin));
        when(identityServiceClient.getUser(userName, token)).thenReturn(Flux.empty());

        Mono<LoginModeResponse> loginModeResponse = userService.getLoginMode(userName);

        StepVerifier.create(loginModeResponse)
                .verifyErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().value() == 404);
        verify(tokenService, times(1)).tokenForAdmin();
        verify(identityServiceClient, times(1)).getUser(userName, token);
        verify(identityServiceClient, times(0)).getCredentials(any(), any());
    }

    @Test
    void shouldReturnCMIdForSingleMatchingRecordForRecoverCMId() {
        String name = "abc";
        Gender gender = Gender.F;
        Integer yearOfBirth = 1999;
        String verifiedIdentifierValue = "+91-8888888888";
        String unverifiedIdentifierValue = "P1234ABCD";
        ArrayList<Identifier> verifiedIdentifiers = new ArrayList<>(Collections.singletonList(new Identifier(IdentifierType.MOBILE, verifiedIdentifierValue)));
        ArrayList<Identifier> unverifiedIdentifiers = new ArrayList<>(Collections.singletonList(new Identifier(IdentifierType.ABPMJAYID, unverifiedIdentifierValue)));
        String cmId = "abc@ncg";
        RecoverCmIdRequest request = new RecoverCmIdRequest(name, gender,yearOfBirth,verifiedIdentifiers,unverifiedIdentifiers);
        JsonArray unverifiedIdentifiersResponse = new JsonArray().add(new JsonObject().put("type","ABPMJAYID").put("value",unverifiedIdentifierValue));
        ArrayList<User> recoverCmIdRows = new ArrayList<>(Collections.singletonList(User.builder().identifier(cmId).name(name).yearOfBirth(yearOfBirth).unverifiedIdentifiers(unverifiedIdentifiersResponse).build()));

        when(userRepository.getCmIdBy(gender,verifiedIdentifierValue)).thenReturn(Mono.just(recoverCmIdRows));

        StepVerifier.create(userService.recoverCmId(request))
                .assertNext(response -> assertThat(response.getCmId()).isEqualTo(cmId))
                .verifyComplete();
        verify(userRepository,times(1)).getCmIdBy(gender,verifiedIdentifierValue);
    }

    @Test
    void shouldThrowAnErrorForMultipleMatchingRecordsForRecoverCMId() {
        String name = "abc";
        Gender gender = Gender.F;
        Integer yearOfBirth = 1999;
        String verifiedIdentifierValue = "+91-8888888888";
        String unverifiedIdentifierValue = "P1234ABCD";
        ArrayList<Identifier> verifiedIdentifiers = new ArrayList<>(Collections.singletonList(new Identifier(IdentifierType.MOBILE, verifiedIdentifierValue)));
        ArrayList<Identifier> unverifiedIdentifiers = new ArrayList<>(Collections.singletonList(new Identifier(IdentifierType.ABPMJAYID, unverifiedIdentifierValue)));
        String cmId = "abc@ncg";
        RecoverCmIdRequest request = new RecoverCmIdRequest(name, gender,yearOfBirth,verifiedIdentifiers,unverifiedIdentifiers);
        JsonArray unverifiedIdentifiersResponse = new JsonArray().add(new JsonObject().put("type","ABPMJAYID").put("value",unverifiedIdentifierValue));
        User recoverCmIdRow = User.builder().identifier(cmId).name(name).yearOfBirth(yearOfBirth).unverifiedIdentifiers(unverifiedIdentifiersResponse).build();
        ArrayList<User> recoverCmIdRows = new ArrayList<>(List.of(recoverCmIdRow, recoverCmIdRow));

        when(userRepository.getCmIdBy(gender,verifiedIdentifierValue)).thenReturn(Mono.just(recoverCmIdRows));

        StepVerifier.create(userService.recoverCmId(request))
                .verifyErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().value() == 404);
        verify(userRepository,times(1)).getCmIdBy(gender,verifiedIdentifierValue);
    }

    @Test
    void shouldThrowAnErrorWhenNoMatchingRecordFoundForRecoverCMId() {
        String name = "abc";
        Gender gender = Gender.F;
        Integer yearOfBirth = 1999;
        String verifiedIdentifierValue = "+91-8888888888";
        String unverifiedIdentifierValue = "P1234ABCD";
        ArrayList<Identifier> verifiedIdentifiers = new ArrayList<>(Collections.singletonList(new Identifier(IdentifierType.MOBILE, verifiedIdentifierValue)));
        ArrayList<Identifier> unverifiedIdentifiers = new ArrayList<>(Collections.singletonList(new Identifier(IdentifierType.ABPMJAYID, unverifiedIdentifierValue)));
        RecoverCmIdRequest request = new RecoverCmIdRequest(name, gender,yearOfBirth,verifiedIdentifiers,unverifiedIdentifiers);
        ArrayList<User> recoverCmIdRows = new ArrayList<>();

        when(userRepository.getCmIdBy(gender,verifiedIdentifierValue)).thenReturn(Mono.just(recoverCmIdRows));

        StepVerifier.create(userService.recoverCmId(request))
                .verifyErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().value() == 404);
        verify(userRepository,times(1)).getCmIdBy(gender,verifiedIdentifierValue);
    }

    @Test
    void shouldThrowAnErrorWhenMendatoryFieldsAreNotProvidedInRequestBodyForRecoverCMId() {
        String name = "abc";
        Gender gender = null;
        Integer yearOfBirth = 1999;
        String verifiedIdentifierValue = "+91-8888888888";
        String unverifiedIdentifierValue = "P1234ABCD";
        ArrayList<Identifier> verifiedIdentifiers = new ArrayList<>(Collections.singletonList(new Identifier(IdentifierType.MOBILE, verifiedIdentifierValue)));
        ArrayList<Identifier> unverifiedIdentifiers = new ArrayList<>(Collections.singletonList(new Identifier(IdentifierType.ABPMJAYID, unverifiedIdentifierValue)));
        RecoverCmIdRequest request = new RecoverCmIdRequest(name, gender,yearOfBirth,verifiedIdentifiers,unverifiedIdentifiers);

        StepVerifier.create(userService.recoverCmId(request))
                .verifyErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().value() == 400);
        verify(userRepository,times(0)).getCmIdBy(gender,verifiedIdentifierValue);
    }

    @Test
    void shouldThrowAnErrorWhenIdentifiersAreMappedIncorrectlyInRequestBodyForRecoverCMId() {
        String name = "abc";
        Gender gender = Gender.F;
        Integer yearOfBirth = 1999;
        String verifiedIdentifierValue = "+91-8888888888";
        String unverifiedIdentifierValue = "P1234ABCD";
        ArrayList<Identifier> verifiedIdentifiers = new ArrayList<>(List.of(new Identifier(IdentifierType.MOBILE, verifiedIdentifierValue), new Identifier(IdentifierType.ABPMJAYID, unverifiedIdentifierValue)));
        ArrayList<Identifier> unverifiedIdentifiers = new ArrayList<>(Collections.singletonList(new Identifier(IdentifierType.ABPMJAYID, unverifiedIdentifierValue)));
        RecoverCmIdRequest request = new RecoverCmIdRequest(name, gender,yearOfBirth,verifiedIdentifiers,unverifiedIdentifiers);

        StepVerifier.create(userService.recoverCmId(request))
                .verifyErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().value() == 400);
        verify(userRepository,times(0)).getCmIdBy(gender,verifiedIdentifierValue);
    }

    @Test
    void shouldThrowAnErrorWhenNoMatchingRecordFoundAndPMJAYIdIsNullInRecordsForRecoverCMId() {
        String name = "abc";
        String cmId = "temp@ncg";
        Gender gender = Gender.F;
        Integer yearOfBirth = 1999;
        String verifiedIdentifierValue = "+91-8888888888";
        String unverifiedIdentifierValue = "P1234ABCD";
        ArrayList<Identifier> verifiedIdentifiers = new ArrayList<>(Collections.singletonList(new Identifier(IdentifierType.MOBILE, verifiedIdentifierValue)));
        ArrayList<Identifier> unverifiedIdentifiers = new ArrayList<>(Collections.singletonList(new Identifier(IdentifierType.ABPMJAYID, unverifiedIdentifierValue)));
        RecoverCmIdRequest request = new RecoverCmIdRequest(name, gender,yearOfBirth,verifiedIdentifiers,unverifiedIdentifiers);
        JsonArray unverifiedIdentifiersResponse = null;
        User recoverCmIdRow = User.builder().identifier(cmId).name(name).yearOfBirth(yearOfBirth).unverifiedIdentifiers(unverifiedIdentifiersResponse).build();
        ArrayList<User> recoverCmIdRows = new ArrayList<>(List.of(recoverCmIdRow));

        when(userRepository.getCmIdBy(gender,verifiedIdentifierValue)).thenReturn(Mono.just(recoverCmIdRows));

        StepVerifier.create(userService.recoverCmId(request))
                .verifyErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().value() == 404);
        verify(userRepository,times(1)).getCmIdBy(gender,verifiedIdentifierValue);
    }
}
