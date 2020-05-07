package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.NullableConverter;
import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.IdentityServiceClient;
import in.projecteka.consentmanager.clients.OtpServiceClient;
import in.projecteka.consentmanager.clients.model.OtpRequest;
import in.projecteka.consentmanager.clients.model.Session;
import in.projecteka.consentmanager.clients.properties.OtpServiceProperties;
import in.projecteka.consentmanager.common.DbOperationError;
import in.projecteka.consentmanager.user.exception.InvalidRequestException;
import in.projecteka.consentmanager.user.model.OtpVerification;
import in.projecteka.consentmanager.user.model.SignUpSession;
import in.projecteka.consentmanager.user.model.Token;
import in.projecteka.consentmanager.user.model.User;
import in.projecteka.consentmanager.user.model.UserSignUpEnquiry;
import in.projecteka.consentmanager.user.model.OtpAttempt;
import org.assertj.core.api.InstanceOfAssertFactories;
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
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.Collections;

import static in.projecteka.consentmanager.user.TestBuilders.coreSignUpRequest;
import static in.projecteka.consentmanager.user.TestBuilders.session;
import static in.projecteka.consentmanager.user.TestBuilders.string;
import static in.projecteka.consentmanager.user.TestBuilders.user;
import static in.projecteka.consentmanager.user.TestBuilders.userSignUpEnquiry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
        var otpServiceProperties = new OtpServiceProperties("", Collections.singletonList("MOBILE"));
        userService = new UserService(
                userRepository,
                otpServiceProperties,
                otpServiceClient,
                signupService,
                identityServiceClient,
                tokenService,
                properties,
                otpAttemptService);
    }

    @Test
    public void shouldReturnTemporarySessionReceivedFromClient() {
        var userSignUpEnquiry = new UserSignUpEnquiry("MOBILE", "+91-9788888");
        var sessionId = string();
        var signUpSession = new SignUpSession(sessionId);
        when(otpServiceClient.send(otpRequestArgumentCaptor.capture())).thenReturn(Mono.empty());
        when(signupService.cacheAndSendSession(sessionCaptor.capture(), eq("+91-9788888")))
                .thenReturn(Mono.just(signUpSession));
        when(otpAttemptService.validateOTPRequest(userSignUpEnquiry.getIdentifier(), OtpAttempt.Action.REGISTRATION)).thenReturn(Mono.empty());

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
        OtpVerification otpVerification = new OtpVerification(sessionId, otp);
        when(otpServiceClient.verify(sessionId, otp)).thenReturn(Mono.empty());
        when(signupService.generateToken(sessionId))
                .thenReturn(Mono.just(new Token(token)));

        StepVerifier.create(userService.permitOtp(otpVerification))
                .assertNext(response -> assertThat(response.getTemporaryToken()).isEqualTo(token))
                .verifyComplete();
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
        Assertions.assertThrows(InvalidRequestException.class, () -> userService.permitOtp(otpVerification));
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
        Assertions.assertThrows(InvalidRequestException.class, () -> userService.permitOtp(otpVerification));
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

}
