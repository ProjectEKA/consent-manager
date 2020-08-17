package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.NullableConverter;
import in.projecteka.consentmanager.clients.IdentityServiceClient;
import in.projecteka.consentmanager.clients.OtpServiceClient;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.clients.model.KeyCloakUserCredentialRepresentation;
import in.projecteka.consentmanager.clients.model.KeyCloakUserRepresentation;
import in.projecteka.consentmanager.clients.model.OtpRequest;
import in.projecteka.consentmanager.clients.model.Session;
import in.projecteka.consentmanager.properties.OtpServiceProperties;
import in.projecteka.consentmanager.common.DbOperationError;
import in.projecteka.consentmanager.consent.ConsentServiceProperties;
import in.projecteka.consentmanager.consent.model.Notification;
import in.projecteka.consentmanager.user.exception.InvalidRequestException;
import in.projecteka.consentmanager.user.model.DateOfBirth;
import in.projecteka.consentmanager.user.model.Gender;
import in.projecteka.consentmanager.user.model.Identifier;
import in.projecteka.consentmanager.user.model.IdentifierType;
import in.projecteka.consentmanager.user.model.InitiateCmIdRecoveryRequest;
import in.projecteka.consentmanager.user.model.LoginMode;
import in.projecteka.consentmanager.user.model.OtpAttempt;
import in.projecteka.consentmanager.user.model.OtpVerification;
import in.projecteka.consentmanager.user.model.PatientName;
import in.projecteka.consentmanager.user.model.PatientResponse;
import in.projecteka.consentmanager.user.model.SendOtpAction;
import in.projecteka.consentmanager.user.model.SignUpSession;
import in.projecteka.consentmanager.user.model.Token;
import in.projecteka.consentmanager.user.model.UpdateUserRequest;
import in.projecteka.consentmanager.user.model.User;
import in.projecteka.consentmanager.user.model.UserSignUpEnquiry;
import in.projecteka.library.clients.model.ClientError;
import in.projecteka.library.clients.model.ErrorCode;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.jeasy.random.EasyRandom;
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
import java.util.UUID;

import static in.projecteka.consentmanager.user.TestBuilders.coreSignUpRequest;
import static in.projecteka.consentmanager.user.TestBuilders.requester;
import static in.projecteka.consentmanager.user.TestBuilders.session;
import static in.projecteka.consentmanager.user.TestBuilders.string;
import static in.projecteka.consentmanager.user.TestBuilders.updatePasswordRequest;
import static in.projecteka.consentmanager.user.TestBuilders.user;
import static in.projecteka.consentmanager.user.TestBuilders.userSignUpEnquiry;
import static in.projecteka.consentmanager.user.model.Requester.HIU;
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

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private ConsentServiceProperties consentServiceProperties;

    @Captor
    private ArgumentCaptor<PatientResponse> patientResponse;

    private UserService userService;

    @BeforeEach
    void setUp() {
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
                lockedUserService,
                userServiceClient,
                consentServiceProperties);
    }

    @Test
    void shouldReturnTemporarySessionReceivedFromClient() {
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
    void shouldThrowInvalidRequestExceptionForInvalidDeviceType() {
        var producer = userService.sendOtp(userSignUpEnquiry().build());

        StepVerifier.create(producer).verifyError(InvalidRequestException.class);
    }

    @Test
    void shouldReturnTokenReceivedFromClient() {
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
    void shouldThrowInvalidRequestExceptionForInvalidOtpValue(
            @ConvertWith(NullableConverter.class) String value) {
        OtpVerification otpVerification = new OtpVerification(string(), value);

        var producer = userService.verifyOtpForRegistration(otpVerification);

        StepVerifier.create(producer).verifyError(InvalidRequestException.class);
    }

    @ParameterizedTest(name = "Invalid session id")
    @CsvSource({
            ",",
            "empty",
            "null"
    })
    void shouldThrowInvalidRequestExceptionForInvalidOtpSessionId(
            @ConvertWith(NullableConverter.class) String sessionId) {
        OtpVerification otpVerification = new OtpVerification(sessionId, string());

        var producer = userService.verifyOtpForRegistration(otpVerification);

        StepVerifier.create(producer).verifyError(InvalidRequestException.class);
    }

    @Test
    void verifyOtp() {
        var sessionId = string();
        var otp = string();
        var token = string();
        var user = new EasyRandom().nextObject(User.class);
        var sessionIdWithAction = SendOtpAction.RECOVER_PASSWORD.toString() + sessionId;
        OtpVerification otpVerification = new OtpVerification(sessionId, otp);
        when(otpServiceClient.verify(eq(sessionId), eq(otp))).thenReturn(Mono.empty());
        when(signupService.generateToken(new HashMap<>(), sessionIdWithAction)).thenReturn(Mono.just(new Token(token)));
        when(signupService.getUserName(eq(sessionIdWithAction))).thenReturn(Mono.just(user.getIdentifier()));
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
    void shouldThrowErrorForInvalidOtpValue(
            @ConvertWith(NullableConverter.class) String value) {
        OtpVerification otpVerification = new OtpVerification(string(), value);

        var producer = userService.verifyOtpForForgetPassword(otpVerification);

        StepVerifier.create(producer).verifyError(InvalidRequestException.class);
    }

    @ParameterizedTest(name = "Invalid session id")
    @CsvSource({
            ",",
            "empty",
            "null"
    })
    void shouldThrowErrorForInvalidOtpSessionId(
            @ConvertWith(NullableConverter.class) String sessionId) {
        OtpVerification otpVerification = new OtpVerification(sessionId, string());

        var producer = userService.verifyOtpForForgetPassword(otpVerification);

        StepVerifier.create(producer).verifyError(InvalidRequestException.class);
    }

    @Test
    void shouldCreateUser() {
        var signUpRequest = coreSignUpRequest()
                .dateOfBirth(DateOfBirth.builder()
                        .month(LocalDate.now().getMonthValue())
                        .year(LocalDate.now().getYear())
                        .build())
                .build();
        var sessionId = string();
        var mobileNumber = string();
        when(tokenService.tokenForAdmin()).thenReturn(Mono.just(new Session()));
        when(signupService.getMobileNumber(sessionId)).thenReturn(Mono.just(mobileNumber));
        when(identityServiceClient.createUser(any(), any())).thenReturn(Mono.empty());
        when(userRepository.save(any())).thenReturn(Mono.empty());
        when(userRepository.userWith(signUpRequest.getUsername())).thenReturn(Mono.empty());

        StepVerifier.create(userService.create(signUpRequest, sessionId))
                .verifyComplete();
    }

    @Test
    void shouldReturnUserAlreadyExistsError() {
        var signUpRequest = coreSignUpRequest()
                .dateOfBirth(DateOfBirth.builder()
                        .month(LocalDate.now().getMonthValue())
                        .year(LocalDate.MIN.getYear())
                        .build())
                .build();
        var sessionId = string();
        var user = user().identifier(signUpRequest.getUsername()).build();
        when(signupService.getMobileNumber(sessionId)).thenReturn(Mono.just(string()));
        when(userRepository.userWith(signUpRequest.getUsername())).thenReturn(Mono.just(user));
        when(userRepository.save(any())).thenReturn(Mono.empty());

        var publisher = userService.create(signUpRequest, sessionId);

        StepVerifier.create(publisher)
                .verifyErrorSatisfies(error -> assertThat(error)
                        .asInstanceOf(InstanceOfAssertFactories.type(ClientError.class))
                        .isEqualToComparingFieldByField(ClientError.userAlreadyExists(signUpRequest.getUsername())));
    }

    @Test
    void shouldCreateUserWhenYOBIsNull() {
        var signUpRequest = coreSignUpRequest()
                .name(
                        PatientName.builder()
                                .first("apoorva")
                                .middle("g")
                                .last("a")
                                .build())
                .dateOfBirth(null)
                .build();
        var sessionId = string();
        var mobileNumber = string();
        when(tokenService.tokenForAdmin()).thenReturn(Mono.just(new Session()));
        when(signupService.getMobileNumber(sessionId)).thenReturn(Mono.just(mobileNumber));
        when(identityServiceClient.createUser(any(), any())).thenReturn(Mono.empty());
        when(userRepository.userWith(signUpRequest.getUsername())).thenReturn(Mono.empty());
        when(userRepository.save(any())).thenReturn(Mono.empty());

        StepVerifier.create(userService.create(signUpRequest, sessionId))
                .verifyComplete();
    }

    @Test
    void shouldNotCreateUserWhenIDPClientFails() {
        var signUpRequest = coreSignUpRequest()
                .dateOfBirth(DateOfBirth.builder()
                        .month(LocalDate.now().getMonthValue())
                        .year(LocalDate.MIN.getYear())
                        .build())
                .build();
        var mobileNumber = string();
        var user = User.from(signUpRequest, mobileNumber);
        var identifier = user.getIdentifier();
        var sessionId = string();
        var tokenForAdmin = session().build();

        when(signupService.getMobileNumber(sessionId)).thenReturn(Mono.just(mobileNumber));
        when(userRepository.userWith(signUpRequest.getUsername())).thenReturn(Mono.empty());
        when(userRepository.save(any())).thenReturn(Mono.empty());
        when(tokenService.tokenForAdmin()).thenReturn(Mono.just(tokenForAdmin));
        when(identityServiceClient.createUser(any(), any()))
                .thenReturn(Mono.error(ClientError.networkServiceCallFailed()));
        when(userRepository.delete(identifier)).thenReturn(Mono.empty());

        var publisher = userService.create(signUpRequest, sessionId);

        StepVerifier.create(publisher).verifyComplete();
        verify(userRepository, times(1)).delete(identifier);
    }

    @Test
    void shouldNotCreateUserWhenPersistingToDbFails() {
        var signUpRequest = coreSignUpRequest()
                .dateOfBirth(DateOfBirth.builder()
                        .month(LocalDate.now().getMonthValue())
                        .year(LocalDate.MIN.getYear())
                        .build())
                .build();
        var mobileNumber = string();
        var user = User.from(signUpRequest, mobileNumber);
        var identifier = user.getIdentifier();
        var sessionId = string();
        var tokenForAdmin = session().build();

        when(signupService.getMobileNumber(sessionId)).thenReturn(Mono.just(mobileNumber));
        when(userRepository.userWith(signUpRequest.getUsername())).thenReturn(Mono.empty());
        when(userRepository.save(any())).thenReturn(Mono.error(new DbOperationError()));
        when(tokenService.tokenForAdmin()).thenReturn(Mono.just(tokenForAdmin));
        when(identityServiceClient.createUser(any(), any())).thenReturn(Mono.empty());
        when(userRepository.delete(identifier)).thenReturn(Mono.empty());

        var publisher = userService.create(signUpRequest, sessionId);

        StepVerifier.create(publisher)
                .verifyErrorSatisfies(error -> assertThat(error)
                        .asInstanceOf(InstanceOfAssertFactories.type(DbOperationError.class))
                        .isEqualToComparingFieldByField(new DbOperationError()));
    }

    @Test
    void updateUserDetails() {
        String userName = "user@ncg";
        var request = UpdateUserRequest.builder().password("Test@3142").build();
        var sessionId = string();
        var tokenForAdmin = session().build();
        var token = String.format("Bearer %s", tokenForAdmin.getAccessToken());
        var userRepresentation = KeyCloakUserRepresentation.builder().id("keycloakuserid").build();
        var newSession = session().build();

        when(signupService.getUserName(sessionId)).thenReturn(Mono.just(userName));
        when(tokenService.tokenForAdmin()).thenReturn(Mono.just(tokenForAdmin));
        when(identityServiceClient.getUser(userName, token)).thenReturn(Mono.just(userRepresentation));
        when(identityServiceClient.updateUser(token, userRepresentation.getId(), request.getPassword()))
                .thenReturn(Mono.empty());
        when(tokenService.tokenForUser(userName, request.getPassword())).thenReturn(Mono.just(newSession));

        var updatedSession = userService.update(request, sessionId);

        StepVerifier.create(updatedSession)
                .assertNext(response -> assertThat(response.getAccessToken()).isEqualTo(newSession.getAccessToken()))
                .verifyComplete();
    }

    @Test
    void updateUserDetailsFailsForInvalidUserName() {
        var request = UpdateUserRequest.builder().password("Test@3142").build();
        var sessionId = string();
        when(signupService.getUserName(sessionId)).thenReturn(Mono.empty());

        var updatedSession = userService.update(request, sessionId);

        StepVerifier.create(updatedSession)
                .verifyErrorMatches(throwable -> throwable instanceof InvalidRequestException &&
                        throwable.getMessage().equals("user not verified"));
    }

    @Test
    void updateUserDetailsFailsWhenTokenForAdminFails() {
        var userName = "user@ncg";
        var request = UpdateUserRequest.builder().password("Test@3142").build();
        var sessionId = string();
        var newSession = session().build();
        when(signupService.getUserName(sessionId)).thenReturn(Mono.just(userName));
        when(tokenService.tokenForAdmin()).thenReturn(Mono.error(ClientError.failedToUpdateUser()));
        when(tokenService.tokenForUser(userName, request.getPassword())).thenReturn(Mono.just(newSession));

        var updatedSession = userService.update(request, sessionId);

        StepVerifier.create(updatedSession)
                .verifyErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().value() == 500);

        verifyNoInteractions(identityServiceClient);
    }

    @Test
    void shouldUpdatePasswordSuccessfully() {
        String userName = "testUser@ncg";
        var request = updatePasswordRequest().build();
        Session oldSession = session().build();
        Session newSession = session().build();
        Session tokenForAdmin = session().build();
        String token = String.format("Bearer %s", tokenForAdmin.getAccessToken());
        var userRepresentation = KeyCloakUserRepresentation.builder().id("keycloakuserid").build();
        when(tokenService.tokenForUser(userName, request.getOldPassword())).thenReturn(Mono.just(oldSession));
        when(tokenService.tokenForAdmin()).thenReturn(Mono.just(tokenForAdmin));
        when(identityServiceClient.getUser(userName, token)).thenReturn(Mono.just(userRepresentation));
        when(identityServiceClient.updateUser(token, userRepresentation.getId(), request.getNewPassword()))
                .thenReturn(Mono.empty());
        when(tokenService.tokenForUser(userName, request.getNewPassword())).thenReturn(Mono.just(newSession));
        when(lockedUserService.validateLogin(userName)).thenReturn(Mono.empty());
        when(lockedUserService.removeLockedUser(userName)).thenReturn(Mono.empty());

        var updatedPasswordSession = userService.updatePassword(request, userName);

        StepVerifier.create(updatedPasswordSession)
                .assertNext(response -> assertThat(response.getAccessToken()).isEqualTo(newSession.getAccessToken()))
                .verifyComplete();

        verify(tokenService, times(1)).tokenForAdmin();
        verify(tokenService, times(1)).tokenForUser(userName, request.getOldPassword());
        verify(identityServiceClient, times(1)).getUser(userName, token);
        verify(identityServiceClient, times(1)).updateUser(token, userRepresentation.getId(), request.getNewPassword());
        verify(tokenService, times(1)).tokenForUser(userName, request.getNewPassword());
    }

    @Test
    void shouldReturnUnauthorizedErrorForInvalidOldPassword() {
        var userName = "TestUser@ncg";
        var request = updatePasswordRequest().build();
        when(tokenService.tokenForUser(userName, request.getOldPassword()))
                .thenReturn(Mono.error(ClientError.unAuthorizedRequest("Invalid Old Password")));
        when(lockedUserService.createOrUpdateLockedUser(userName)).thenReturn(Mono.just(2));

        var updatedPasswordSession = userService.updatePassword(request, userName);

        StepVerifier.create(updatedPasswordSession)
                .verifyErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().value() == 401);
        verify(tokenService, times(1)).tokenForUser(userName, request.getOldPassword());
        verifyNoInteractions(identityServiceClient);
    }

    @Test
    void shouldReturnErrorWhenUpdateUserWithNewPasswordFails() {
        var userName = "user@ncg";
        var request = updatePasswordRequest().build();
        var oldSession = session().build();
        var newSession = session().build();
        var tokenForAdmin = session().build();
        var token = String.format("Bearer %s", tokenForAdmin.getAccessToken());
        var userRepresentation = KeyCloakUserRepresentation.builder().id("keycloakuserid").build();
        when(tokenService.tokenForUser(userName, request.getOldPassword())).thenReturn(Mono.just(oldSession));
        when(tokenService.tokenForAdmin()).thenReturn(Mono.just(tokenForAdmin));
        when(identityServiceClient.getUser(userName, token)).thenReturn(Mono.just(userRepresentation));
        when(identityServiceClient.updateUser(token, userRepresentation.getId(), request.getNewPassword()))
                .thenReturn(Mono.error(ClientError.failedToUpdateUser()));
        when(tokenService.tokenForUser(userName, request.getNewPassword())).thenReturn(Mono.just(newSession));
        when(lockedUserService.validateLogin(userName)).thenReturn(Mono.empty());
        when(lockedUserService.removeLockedUser(userName)).thenReturn(Mono.empty());

        Mono<Session> updatedPasswordSession = userService.updatePassword(request, userName);

        StepVerifier.create(updatedPasswordSession)
                .verifyErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().value() == 500);

        verify(tokenService, times(1)).tokenForAdmin();
        verify(tokenService, times(1)).tokenForUser(userName, request.getOldPassword());
        verify(identityServiceClient, times(1)).getUser(userName, token);
        verify(identityServiceClient, times(1)).updateUser(token, userRepresentation.getId(), request.getNewPassword());
        verify(tokenService, times(1)).tokenForUser(userName, request.getNewPassword());
    }

    @Test
    void getLoginMode() {
        String userName = "user@ncg";
        Session tokenForAdmin = session().build();
        String token = String.format("Bearer %s", tokenForAdmin.getAccessToken());
        var userRepresentation = KeyCloakUserRepresentation.builder().id("keycloakuserid").build();
        var userCreds = Mono.just(KeyCloakUserCredentialRepresentation
                .builder()
                .id("credid")
                .type("password")
                .build());
        when(tokenService.tokenForAdmin()).thenReturn(Mono.just(tokenForAdmin));
        when(identityServiceClient.getUser(userName, token)).thenReturn(Mono.just(userRepresentation));
        when(identityServiceClient.getCredentials(userRepresentation.getId(), token)).thenReturn(userCreds);

        var loginModeResponse = userService.getLoginMode(userName);

        StepVerifier.create(loginModeResponse)
                .assertNext(response -> assertThat(response.getLoginMode()).isEqualTo(LoginMode.CREDENTIAL))
                .verifyComplete();
        verify(tokenService, times(1)).tokenForAdmin();
        verify(identityServiceClient, times(1)).getUser(userName, token);
        verify(identityServiceClient, times(1)).getCredentials(userRepresentation.getId(), token);
    }

    @Test
    void getLoginModeAsOTPWhenPasswordNotSet() {
        String userName = "user@ncg";
        Session tokenForAdmin = session().build();
        String token = String.format("Bearer %s", tokenForAdmin.getAccessToken());
        var userRepresentation = KeyCloakUserRepresentation.builder().id("keycloakuserid").build();
        when(tokenService.tokenForAdmin()).thenReturn(Mono.just(tokenForAdmin));
        when(identityServiceClient.getUser(userName, token)).thenReturn(Mono.just(userRepresentation));
        when(identityServiceClient.getCredentials(userRepresentation.getId(), token)).thenReturn(Mono.empty());

        var loginModeResponse = userService.getLoginMode(userName);

        StepVerifier.create(loginModeResponse)
                .assertNext(response -> assertThat(response.getLoginMode()).isEqualTo(LoginMode.OTP))
                .verifyComplete();
        verify(tokenService, times(1)).tokenForAdmin();
        verify(identityServiceClient, times(1)).getUser(userName, token);
        verify(identityServiceClient, times(1)).getCredentials(userRepresentation.getId(), token);
    }

    @Test
    void getLoginModeReturnsErrorForNonExistentUser() {
        String userName = "user@ncg";
        Session tokenForAdmin = session().build();
        String token = String.format("Bearer %s", tokenForAdmin.getAccessToken());
        when(tokenService.tokenForAdmin()).thenReturn(Mono.just(tokenForAdmin));
        when(identityServiceClient.getUser(userName, token)).thenReturn(Mono.empty());

        var loginModeResponse = userService.getLoginMode(userName);

        StepVerifier.create(loginModeResponse)
                .verifyErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().value() == 404);
        verify(tokenService, times(1)).tokenForAdmin();
        verify(identityServiceClient, times(1)).getUser(userName, token);
        verify(identityServiceClient, times(0)).getCredentials(any(), any());
    }

    @Test
    void shouldReturnCMIdForSingleMatchingRecordForInitiateRecoverCMId() {
        PatientName name = PatientName.builder().first("abc").middle(null).last(null).build();
        Gender gender = Gender.F;
        DateOfBirth dateOfBirth = DateOfBirth.builder().year(1999).build();
        String verifiedIdentifierValue = "+91-8888888888";
        String unverifiedIdentifierValue = "P1234ABCD";
        var verifiedIdentifiers = new ArrayList<>(Collections.singletonList(new Identifier(IdentifierType.MOBILE, verifiedIdentifierValue)));
        var unverifiedIdentifiers = new ArrayList<>(Collections.singletonList(new Identifier(IdentifierType.ABPMJAYID, unverifiedIdentifierValue)));
        String cmId = "abc@ncg";
        var request = new InitiateCmIdRecoveryRequest(name, gender, dateOfBirth, verifiedIdentifiers, unverifiedIdentifiers);
        var unverifiedIdentifiersResponse = new JsonArray().add(new JsonObject().put("type", "ABPMJAYID").put("value", unverifiedIdentifierValue));
        var recoverCmIdRows = new ArrayList<>(Collections.singletonList(User.builder().identifier(cmId).phone(verifiedIdentifierValue).name(name).dateOfBirth(dateOfBirth).unverifiedIdentifiers(unverifiedIdentifiersResponse).build()));

        when(userRepository.getUserBy(gender, verifiedIdentifierValue)).thenReturn(Flux.fromIterable(recoverCmIdRows));

        StepVerifier.create(userService.getPatientByDetails(request))
                .assertNext(response -> {
                    assertThat(response.getIdentifier()).isEqualTo(cmId);
                    assertThat(response.getName().createFullName()).isEqualTo(name.createFullName());
                    assertThat(response.getPhone()).isEqualTo(verifiedIdentifierValue);
                    assertThat(response.getDateOfBirth().getYear()).isEqualTo(dateOfBirth.getYear());
                })
                .verifyComplete();
        verify(userRepository, times(1)).getUserBy(gender, verifiedIdentifierValue);
    }

    @Test
    void shouldReturnEmptyMonoForMultipleMatchingRecordsForInitiateRecoverCMId() {
        PatientName name = PatientName.builder().first("abc").middle("").last(null).build();
        Gender gender = Gender.F;
        DateOfBirth dateOfBirth = DateOfBirth.builder().year(1999).build();
        String verifiedIdentifierValue = "+91-8888888888";
        String unverifiedIdentifierValue = "P1234ABCD";
        var verifiedIdentifiers = new ArrayList<>(Collections.singletonList(new Identifier(IdentifierType.MOBILE, verifiedIdentifierValue)));
        var unverifiedIdentifiers = new ArrayList<>(Collections.singletonList(new Identifier(IdentifierType.ABPMJAYID, unverifiedIdentifierValue)));
        String cmId = "abc@ncg";
        var request = new InitiateCmIdRecoveryRequest(name, gender, dateOfBirth, verifiedIdentifiers, unverifiedIdentifiers);
        JsonArray unverifiedIdentifiersResponse = new JsonArray().add(new JsonObject().put("type", "ABPMJAYID").put("value", unverifiedIdentifierValue));
        User recoverCmIdRow = User.builder().identifier(cmId).name(name).dateOfBirth(dateOfBirth).unverifiedIdentifiers(unverifiedIdentifiersResponse).build();
        var recoverCmIdRows = new ArrayList<>(List.of(recoverCmIdRow, recoverCmIdRow));

        when(userRepository.getUserBy(gender, verifiedIdentifierValue)).thenReturn(Flux.fromIterable(recoverCmIdRows));

        StepVerifier.create(userService.getPatientByDetails(request))
                .verifyComplete();
        verify(userRepository, times(1)).getUserBy(gender, verifiedIdentifierValue);
    }

    @Test
    void shouldThrowAnErrorWhenNoMatchingRecordFoundAndPMJAYIdIsNullInRecordsForInitiateRecoverCMId() {
        PatientName name = PatientName.builder().first("abc").middle("").last(null).build();
        Gender gender = Gender.F;
        DateOfBirth dateOfBirth = DateOfBirth.builder().year(1999).build();
        String verifiedIdentifierValue = "+91-8888888888";
        String unverifiedIdentifierValue = "P1234ABCD";
        ArrayList<Identifier> verifiedIdentifiers = new ArrayList<>(Collections.singletonList(new Identifier(IdentifierType.MOBILE, verifiedIdentifierValue)));
        ArrayList<Identifier> unverifiedIdentifiers = new ArrayList<>(Collections.singletonList(new Identifier(IdentifierType.ABPMJAYID, unverifiedIdentifierValue)));
        String cmId = "abc@ncg";
        InitiateCmIdRecoveryRequest request = new InitiateCmIdRecoveryRequest(name, gender, dateOfBirth, verifiedIdentifiers, unverifiedIdentifiers);
        JsonArray unverifiedIdentifiersResponse = null;
        User recoverCmIdRow = User.builder().identifier(cmId).name(name).dateOfBirth(dateOfBirth).unverifiedIdentifiers(unverifiedIdentifiersResponse).build();
        ArrayList<User> recoverCmIdRows = new ArrayList<>(List.of(recoverCmIdRow));

        when(userRepository.getUserBy(gender, verifiedIdentifierValue)).thenReturn(Flux.fromIterable(recoverCmIdRows));

        StepVerifier.create(userService.getPatientByDetails(request))
                .verifyComplete();
        verify(userRepository, times(1)).getUserBy(gender, verifiedIdentifierValue);
    }

    @Test
    void verifyOtpForRecoveringCmId() {
        var sessionId = string();
        var otp = string();
        var token = string();
        var user = new EasyRandom().nextObject(User.class);
        var sessionIdWithAction = SendOtpAction.RECOVER_CM_ID.toString() + sessionId;
        ArgumentCaptor<OtpAttempt> argument = ArgumentCaptor.forClass(OtpAttempt.class);
        ArgumentCaptor<Notification> consentManagerArgumentCaptor = ArgumentCaptor.forClass(Notification.class);
        OtpVerification otpVerification = new OtpVerification(sessionId, otp);
        when(otpServiceClient.verify(eq(sessionId), eq(otp))).thenReturn(Mono.empty());
        when(signupService.generateToken(new HashMap<>(), sessionId))
                .thenReturn(Mono.just(new Token(token)));
        when(signupService.getUserName(eq(sessionIdWithAction))).thenReturn(Mono.just(user.getIdentifier()));
        when(userRepository.userWith(eq(user.getIdentifier()))).thenReturn(Mono.just(user));
        when(otpAttemptService.validateOTPSubmission(argument.capture())).thenReturn(Mono.empty());
        when(otpAttemptService.removeMatchingAttempts(argument.capture())).thenReturn(Mono.empty());
        when(otpServiceClient.send(consentManagerArgumentCaptor.capture())).thenReturn(Mono.empty());
        StepVerifier.create(userService.verifyOtpForRecoverCmId(otpVerification))
                .assertNext(response -> assertThat(response.getCmId()).isEqualTo(user.getIdentifier()))
                .verifyComplete();
    }

    @Test
    void shouldThrowErrorForInvalidOtpValueForRecoverCmId() {
        OtpVerification otpVerification = new OtpVerification(string(), null);

        var producer = userService.verifyOtpForRecoverCmId(otpVerification);

        StepVerifier.create(producer).verifyError(InvalidRequestException.class);
    }

    @Test
    void shouldThrowErrorForInvalidOtpSessionIdForRecoverCmId() {
        OtpVerification otpVerification = new OtpVerification(null, string());

        var producer = userService.verifyOtpForRecoverCmId(otpVerification);

        StepVerifier.create(producer).verifyError(InvalidRequestException.class);
    }

    @Test
    void callGateWayWhenUserNotFound() {
        var userName = string();
        var requester = requester().type(HIU).build();
        var requestId = UUID.randomUUID();
        when(userRepository.userWith(any())).thenReturn(Mono.empty());
        when(userServiceClient.sendPatientResponseToGateWay(patientResponse.capture(),
                eq("X-HIU-ID"),
                eq(requester.getId())))
                .thenReturn(Mono.empty());

        var patientProducer = userService.user(userName, requester, requestId);

        StepVerifier.create(patientProducer)
                .verifyComplete();
        verify(userRepository, times(1)).userWith(any());
        verify(userServiceClient, times(1)).sendPatientResponseToGateWay(any(), any(), any());
        assertThat(patientResponse.getValue().getError()).isNotNull();
        assertThat(patientResponse.getValue().getError().getCode()).isEqualTo(ErrorCode.USER_NOT_FOUND.getValue());
        assertThat(patientResponse.getValue().getPatient()).isNull();
    }

    @Test
    void callGateWayWhenUserFound() {
        var userName = string();
        var requester = requester().type(HIU).build();
        var requestId = UUID.randomUUID();
        var user = user().build();
        when(userRepository.userWith(any())).thenReturn(Mono.just(user));
        when(userServiceClient.sendPatientResponseToGateWay(patientResponse.capture(),
                eq("X-HIU-ID"),
                eq(requester.getId())))
                .thenReturn(Mono.empty());

        var patientProducer = userService.user(userName, requester, requestId);

        StepVerifier.create(patientProducer)
                .verifyComplete();
        verify(userRepository, times(1)).userWith(any());
        verify(userServiceClient, times(1)).sendPatientResponseToGateWay(any(), any(), any());
        assertThat(patientResponse.getValue().getError()).isNull();
        assertThat(patientResponse.getValue().getPatient()).isNotNull();
    }

    @Test
    void shouldVerifyOTPForForgetConsentPin() {
        var sessionId = string();
        var otp = string();
        var token = string();
        var user = new EasyRandom().nextObject(User.class);
        var sessionIdWithAction = SendOtpAction.FORGOT_CONSENT_PIN.toString() + sessionId;
        ArgumentCaptor<OtpAttempt> argument = ArgumentCaptor.forClass(OtpAttempt.class);
        OtpVerification otpVerification = new OtpVerification(sessionId, otp);
        when(otpServiceClient.verify(eq(sessionId), eq(otp))).thenReturn(Mono.empty());
        when(signupService.generateToken(any(), eq(sessionIdWithAction))).thenReturn(Mono.just(new Token(token)));
        when(signupService.getUserName(eq(sessionIdWithAction))).thenReturn(Mono.just(user.getIdentifier()));
        when(userRepository.userWith(eq(user.getIdentifier()))).thenReturn(Mono.just(user));
        when(otpAttemptService.validateOTPSubmission(argument.capture())).thenReturn(Mono.empty());
        when(otpAttemptService.removeMatchingAttempts(argument.capture())).thenReturn(Mono.empty());

        StepVerifier.create(userService.verifyOtpForForgotConsentPin(otpVerification))
                .assertNext(response -> assertThat(response.getTemporaryToken()).isEqualTo(token))
                .verifyComplete();
    }
}
