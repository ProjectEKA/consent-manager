package in.projecteka.consentmanager.user;

import com.google.common.base.Verify;
import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.HASSignupServiceClient;
import in.projecteka.consentmanager.clients.IdentityServiceClient;
import in.projecteka.consentmanager.clients.model.KeycloakUser;
import in.projecteka.consentmanager.clients.model.Session;
import in.projecteka.consentmanager.clients.properties.OtpServiceProperties;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import in.projecteka.consentmanager.user.model.DateOfBirth;
import in.projecteka.consentmanager.user.model.Gender;
import in.projecteka.consentmanager.user.model.HASSignupRequest;
import in.projecteka.consentmanager.user.model.HealthAccountUser;
import in.projecteka.consentmanager.user.model.PatientName;
import in.projecteka.consentmanager.user.model.SessionRequest;
import in.projecteka.consentmanager.user.model.SignUpRequest;
import in.projecteka.consentmanager.user.model.UpdateHASUserRequest;
import in.projecteka.consentmanager.user.model.UpdateLoginDetailsRequest;
import in.projecteka.consentmanager.user.model.GenerateAadharOtpResponse;
import in.projecteka.consentmanager.user.model.GenerateAadharOtpRequest;
import in.projecteka.consentmanager.user.model.User;
import in.projecteka.consentmanager.user.model.VerifyAadharOtpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static in.projecteka.consentmanager.user.TestBuilders.session;
import static in.projecteka.consentmanager.user.TestBuilders.string;
import static in.projecteka.consentmanager.user.TestBuilders.user;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

class HASSignupServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SignUpService signupService;

    @Mock
    private HASSignupServiceClient hasSignupServiceClient;

    @Mock
    private TokenService tokenService;

    @Mock
    private IdentityServiceClient identityServiceClient;

    @Mock
    private SessionService sessionService;

    @Mock
    private OtpServiceProperties otpServiceProperties;

    @Mock
    private DummyHealthAccountService dummyHealthAccountService;

    private HASSignupService hasSignupService;

    @Mock
    private CacheAdapter<String, String> hasCache;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        hasSignupService = new HASSignupService(
                hasSignupServiceClient,
                userRepository,
                signupService,
                tokenService,
                identityServiceClient,
                sessionService,
                otpServiceProperties,
                dummyHealthAccountService,
                hasCache);
    }

    @Test
    public void shouldCreateNewUserOnHAS() {
        var signUpRequest = SignUpRequest.builder()
                .name(PatientName.builder().first("hina").middle("").last("patel").build())
                .dateOfBirth(DateOfBirth.builder().date(1).month(1).year(2020).build())
                .gender(Gender.F)
                .build();
        var hasUser = HealthAccountUser.builder()
                .firstName("hina")
                .middleName("")
                .lastName("patel")
                .dayOfBirth(1)
                .monthOfBirth(1)
                .yearOfBirth(2020)
                .healthId("tempId")
                .name("hina patel")
                .gender("F")
                .token("tempToken")
                .build();
        var token = string();
        var mobileNumber = string();

        when(userRepository.getPatientByHealthId(anyString())).thenReturn(Mono.empty());
        when(hasSignupServiceClient.createHASAccount(any(HASSignupRequest.class))).thenReturn(Mono.just(hasUser));
        when(signupService.getSessionId(anyString())).thenReturn(token);
        when(signupService.getMobileNumber(anyString())).thenReturn(Mono.just(mobileNumber));
        when(userRepository.save(any(), anyString())).thenReturn(Mono.empty());
        when(signupService.removeOf(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(hasSignupService.createHASAccount(signUpRequest, token))
                .assertNext(res -> {
                    assertThat(res.getHealthId()).isEqualTo("tempId");
                    assertThat(res.getToken()).isEqualTo("tempToken");
                    assertThat(res.getCmId()).isEqualTo(null);
                })
                .verifyComplete();
    }

    @Test
    public void shouldCreateNewUserForAllowedNumber() {
        var signUpRequest = SignUpRequest.builder()
                .name(PatientName.builder().first("hina").middle("").last("patel").build())
                .dateOfBirth(DateOfBirth.builder().date(1).month(1).year(2020).build())
                .gender(Gender.F)
                .build();
        var token = string();
        var mobileNumber = "+91-9999999999";
        HealthAccountUser healthAccountUser = HealthAccountUser.builder().newHASUser(true)
                .healthId(UUID.randomUUID().toString())
                .token(UUID.randomUUID().toString())
                .firstName("Hina")
                .lastName("Patel")
                .middleName("")
                .dayOfBirth(6)
                .monthOfBirth(12)
                .yearOfBirth(1960)
                .gender("F")
                .newHASUser(true)
                .build();

        var user = user();

        when(signupService.getSessionId(anyString())).thenReturn(token);
        when(signupService.getMobileNumber(anyString())).thenReturn(Mono.just(mobileNumber));
        when(userRepository.save(any(HealthAccountUser.class), anyString())).thenReturn(Mono.empty());
        when(signupService.removeOf(anyString())).thenReturn(Mono.empty());
        when(otpServiceProperties.allowListNumbers()).thenReturn(Collections.singletonList("+91-9999999999"));
        when(dummyHealthAccountService.createHASAccount(any(HASSignupRequest.class))).thenReturn(Mono.just(healthAccountUser));
        when(userRepository.getPatientByHealthId(anyString())).thenReturn(Mono.empty());


        StepVerifier.create(hasSignupService.createHASAccount(signUpRequest, token))
                .assertNext(res -> {
                    assertThat(res.getHealthId()).isEqualTo(healthAccountUser.getHealthId());
                    assertThat(res.getToken()).isEqualTo(healthAccountUser.getToken());
                    assertThat(res.getNewHASUser()).isTrue();
                })
                .verifyComplete();
        verify(hasSignupServiceClient, never()).createHASAccount(any(HASSignupRequest.class));
    }

    @Test
    public void shouldThrowErrorWhenUnableToCreateUserOnHAS() {
        var signUpRequest = SignUpRequest.builder()
                .name(PatientName.builder().first("hina").middle("").last("patel").build())
                .dateOfBirth(DateOfBirth.builder().date(1).month(1).year(2020).build())
                .gender(Gender.F)
                .build();
        var token = string();
        var mobileNumber = string();

        when(hasSignupServiceClient.createHASAccount(any(HASSignupRequest.class)))
                .thenReturn(Mono.error(ClientError.networkServiceCallFailed()));
        when(signupService.getSessionId(anyString())).thenReturn(token);
        when(signupService.getMobileNumber(anyString())).thenReturn(Mono.just(mobileNumber));

        StepVerifier.create(hasSignupService.createHASAccount(signUpRequest, token))
                .expectErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().is5xxServerError())
                .verify();
    }

    @Test
    public void shouldThrowErrorWhenTokenIsInvalid() {
        var signUpRequest = SignUpRequest.builder()
                .name(PatientName.builder().first("hina").middle("").last("patel").build())
                .dateOfBirth(DateOfBirth.builder().date(1).month(1).year(2020).build())
                .gender(Gender.F)
                .build();
        var token = string();
        var mobileNumber = string();

        when(hasSignupServiceClient.createHASAccount(any(HASSignupRequest.class)))
                .thenReturn(Mono.error(ClientError.unAuthorized()));
        when(signupService.getSessionId(anyString())).thenReturn(token);
        when(signupService.getMobileNumber(anyString())).thenReturn(Mono.just(mobileNumber));

        StepVerifier.create(hasSignupService.createHASAccount(signUpRequest, token))
                .expectErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().is4xxClientError())
                .verify();
    }

    @Test
    public void shouldReturnCMIdForExistingUser() {
        var signUpRequest = SignUpRequest.builder()
                .name(PatientName.builder().first("hina").middle("").last("patel").build())
                .dateOfBirth(DateOfBirth.builder().date(1).month(1).year(2020).build())
                .gender(Gender.F)
                .build();
        var hasUser = HealthAccountUser.builder()
                .firstName("hina")
                .middleName("")
                .lastName("patel")
                .dayOfBirth(1)
                .monthOfBirth(1)
                .yearOfBirth(2020)
                .healthId("tempId")
                .name("hina patel")
                .gender("F")
                .token("tempToken")
                .newHASUser(false)
                .build();
        var user = User.builder().identifier("hinapatel00@pmjay").build();
        var token = string();
        String mobileNumber = string();

        when(signupService.getSessionId(anyString())).thenReturn(token);
        when(signupService.getMobileNumber(anyString())).thenReturn(Mono.just(mobileNumber));
        when(hasSignupServiceClient.createHASAccount(any(HASSignupRequest.class))).thenReturn(Mono.just(hasUser));
        when(userRepository.getPatientByHealthId(anyString())).thenReturn(Mono.just(user));
        when(signupService.removeOf(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(hasSignupService.createHASAccount(signUpRequest, token))
                .assertNext(res -> {
                    assertThat(res.getHealthId()).isEqualTo("tempId");
                    assertThat(res.getToken()).isEqualTo("tempToken");
                    assertThat(res.getCmId()).isEqualTo("hinapatel00@pmjay");
                    assertThat(res.getNewHASUser()).isEqualTo(false);
                })
                .verifyComplete();
        verify(userRepository, never()).save(any(HealthAccountUser.class), anyString());
    }

    @Test
    public void shouldReturnCMIdForExistingUserInCaseOfAllowedNumber() {
        var signUpRequest = SignUpRequest.builder()
                .name(PatientName.builder().first("hina").middle("").last("patel").build())
                .dateOfBirth(DateOfBirth.builder().date(1).month(1).year(2020).build())
                .gender(Gender.F)
                .build();
        var user = User.builder().identifier("hinapatel00@pmjay").healthId(UUID.randomUUID().toString())
                .name(PatientName.builder().first("Hina").last("Patel").middle("").build())
                .dateOfBirth(DateOfBirth.builder().date(7).month(4).year(1979).build())
                .gender(Gender.valueOf("F"))
                .build();
        var token = string();
        var mobileNumber = "+91-9999999999";
        HealthAccountUser healthAccountUser = HealthAccountUser.builder().newHASUser(false)
                .healthId(UUID.randomUUID().toString())
                .token(UUID.randomUUID().toString())
                .firstName("Hina")
                .lastName("Patel")
                .middleName("")
                .dayOfBirth(6)
                .monthOfBirth(12)
                .yearOfBirth(1960)
                .gender("F")
                .build();

        when(signupService.getSessionId(anyString())).thenReturn(token);
        when(signupService.getMobileNumber(anyString())).thenReturn(Mono.just(mobileNumber));
        when(otpServiceProperties.allowListNumbers()).thenReturn(Arrays.asList("+91-9999999999"));
        when(userRepository.getPatientByHealthId(anyString())).thenReturn(Mono.just(user));
        when(dummyHealthAccountService.createHASAccount(any(HASSignupRequest.class)))
                .thenReturn(Mono.just(healthAccountUser));
        when(signupService.removeOf(anyString())).thenReturn(Mono.empty());


        StepVerifier.create(hasSignupService.createHASAccount(signUpRequest, token))
                .assertNext(res -> {
                    assertThat(res.getHealthId()).isEqualTo(healthAccountUser.getHealthId());
                    assertThat(res.getToken()).isEqualTo(healthAccountUser.getToken());
                    assertThat(res.getNewHASUser()).isFalse();
                })
                .verifyComplete();
        verify(userRepository, never()).save(any(HealthAccountUser.class), anyString());
    }

    @Test
    public void shouldUpdateLoginDetailsOnHAS() {
        var updateLoginRequestDetails = UpdateLoginDetailsRequest.builder().cmId("hinapatel456@pmjay")
                .healthId("12345-12345-12345")
                .password("Test@1243")
                .build();
        var token = string();
        var user = User.builder().name(PatientName.builder().first("hina").middle("").last("patel").build()).build();
        var session = session().build();
        String accessToken = format("Bearer %s", session.getAccessToken());

        when(userRepository.userWith(updateLoginRequestDetails.getCmId())).thenReturn(Mono.empty());
        when(hasSignupServiceClient.updateHASAccount(any(UpdateHASUserRequest.class))).thenReturn(Mono.empty());
        when(userRepository.updateCMId(anyString(), anyString()))
                .thenReturn(Mono.empty());
        when(userRepository.getPatientByHealthId(updateLoginRequestDetails.getHealthId()))
                .thenReturn(Mono.just(user));
        when(tokenService.tokenForAdmin()).thenReturn(Mono.just(session));
        when(identityServiceClient.createUser(any(Session.class), any(KeycloakUser.class))).thenReturn(Mono.empty());
        when(sessionService.forNew(any(SessionRequest.class))).thenReturn(Mono.just(session));

        StepVerifier.create(hasSignupService.updateHASLoginDetails(updateLoginRequestDetails, token))
                .assertNext(res -> {
                    assertThat(res.getToken()).isEqualTo(accessToken);
                })
                .verifyComplete();
    }

    @Test
    public void shouldUpdateLoginDetailsForAllowedNumber() {
        var updateLoginRequestDetails = UpdateLoginDetailsRequest.builder().cmId("hinapatel456@pmjay")
                .healthId("12345-12345-12345")
                .password("Test@1243")
                .build();
        var token = UUID.randomUUID().toString();
        var user = User.builder().name(PatientName.builder().first("hina").middle("").last("patel").build()).build();
        var session = session().build();
        String accessToken = format("Bearer %s", session.getAccessToken());

        when(userRepository.userWith(updateLoginRequestDetails.getCmId())).thenReturn(Mono.empty());
        when(userRepository.updateCMId(anyString(), anyString()))
                .thenReturn(Mono.empty());
        when(userRepository.getPatientByHealthId(updateLoginRequestDetails.getHealthId()))
                .thenReturn(Mono.just(user));
        when(tokenService.tokenForAdmin()).thenReturn(Mono.just(session));
        when(identityServiceClient.createUser(any(Session.class), any(KeycloakUser.class))).thenReturn(Mono.empty());
        when(sessionService.forNew(any(SessionRequest.class))).thenReturn(Mono.just(session));

        StepVerifier.create(hasSignupService.updateHASLoginDetails(updateLoginRequestDetails, token))
                .assertNext(res -> {
                    assertThat(res.getToken()).isEqualTo(accessToken);
                })
                .verifyComplete();
        verify(hasSignupServiceClient, never()).updateHASAccount(any(UpdateHASUserRequest.class));
    }

    @Test
    public void shouldThrowErrorWhenUserAlreadyExists() {
        var updateLoginRequestDetails = UpdateLoginDetailsRequest.builder().cmId("hinapatel456@pmjay")
                .healthId("12345-12345-12345")
                .password("Test@1243")
                .build();
        var token = string();
        var user = user().build();

        when(userRepository.userWith(anyString()))
                .thenReturn(Mono.just(user));

        StepVerifier.create(hasSignupService.updateHASLoginDetails(updateLoginRequestDetails, token))
                .expectErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().is4xxClientError())
                .verify();
    }

    @Test
    public void shouldThrowErrorWhenUnableToUpdatePasswordOnHAS() {
        var updateLoginRequestDetails = UpdateLoginDetailsRequest.builder().cmId("hinapatel456@pmjay")
                .healthId("12345-12345-12345")
                .password("Test@1243")
                .build();
        var token = string();

        when(userRepository.userWith(updateLoginRequestDetails.getCmId())).thenReturn(Mono.empty());
        when(hasSignupServiceClient.updateHASAccount(any(UpdateHASUserRequest.class)))
                .thenReturn(Mono.error(ClientError.networkServiceCallFailed()));

        StepVerifier.create(hasSignupService.updateHASLoginDetails(updateLoginRequestDetails, token))
                .expectErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().is5xxServerError())
                .verify();
    }

    @Test
    public void shouldThrowErrorWhenHealthIdNotFound() {
        var updateLoginRequestDetails = UpdateLoginDetailsRequest.builder().cmId("hinapatel456@pmjay")
                .healthId("12345-12345-12345")
                .password("Test@1243")
                .build();
        var token = string();

        when(userRepository.userWith(updateLoginRequestDetails.getCmId())).thenReturn(Mono.empty());
        when(hasSignupServiceClient.updateHASAccount(any(UpdateHASUserRequest.class))).thenReturn(Mono.empty());
        when(userRepository.updateCMId(anyString(), anyString()))
                .thenReturn(Mono.empty());
        when(userRepository.getPatientByHealthId(updateLoginRequestDetails.getHealthId()))
                .thenReturn(Mono.empty());

        StepVerifier.create(hasSignupService.updateHASLoginDetails(updateLoginRequestDetails, token))
                .expectErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().is4xxClientError())
                .verify();
    }

    @Test
    public void shouldThrowErrorWhenUserNameOrPasswordIsInvalid() {
        var updateLoginRequestDetails = UpdateLoginDetailsRequest.builder().cmId("hinapatel456@pmjay")
                .healthId("12345-12345-12345")
                .password("Test@1243")
                .build();
        var token = string();
        var user = User.builder().name(PatientName.builder().first("hina").middle("").last("patel").build()).build();
        var session = session().build();


        when(userRepository.userWith(updateLoginRequestDetails.getCmId())).thenReturn(Mono.empty());
        when(hasSignupServiceClient.updateHASAccount(any(UpdateHASUserRequest.class))).thenReturn(Mono.empty());
        when(userRepository.updateCMId(anyString(), anyString()))
                .thenReturn(Mono.empty());
        when(userRepository.getPatientByHealthId(updateLoginRequestDetails.getHealthId()))
                .thenReturn(Mono.just(user));
        when(tokenService.tokenForAdmin()).thenReturn(Mono.just(session));
        when(identityServiceClient.createUser(any(Session.class), any(KeycloakUser.class)))
                .thenReturn(Mono.empty());
        when(sessionService.forNew(any(SessionRequest.class))).thenReturn(Mono.error(ClientError.invalidUserNameOrPassword()));

        StepVerifier.create(hasSignupService.updateHASLoginDetails(updateLoginRequestDetails, token))
                .expectErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().is4xxClientError())
                .verify();
    }

    @Test
    public void shouldGenerateAadharOTP() {
        var token = string();
        var aadharOtpRequest = GenerateAadharOtpRequest.builder().aadhaar("123456789012").build();
        var aadharOtpResponse = GenerateAadharOtpResponse.builder().txnID("testTxnID").token(token).build();
        var sessionId = string();
        var mobileNumber = string();

        when(signupService.getSessionId(token)).thenReturn(sessionId);
        when(signupService.getMobileNumber(sessionId)).thenReturn(Mono.just(mobileNumber));
        when(hasSignupServiceClient.generateAadharOtp(aadharOtpRequest)).thenReturn(Mono.just(aadharOtpResponse));

        StepVerifier.create(hasSignupService.generateAadharOtp(aadharOtpRequest,token))
                .assertNext(response -> {
                    assertThat(response.getTxnID()).isEqualTo("testTxnID");
                    assertThat(response.getToken()).isEqualTo(token);
                })
                .verifyComplete();
    }

    @Test
    public void shouldThrowErrorWhenAadharIsInvalid() {
        var token = string();
        var aadharOtpRequest = GenerateAadharOtpRequest.builder().aadhaar("12345678901").build();
        var aadharOtpResponse = GenerateAadharOtpResponse.builder().txnID("testTxnID").token(token).build();
        var sessionId = string();
        var mobileNumber = string();

        when(signupService.getSessionId(token)).thenReturn(sessionId);
        when(signupService.getMobileNumber(sessionId)).thenReturn(Mono.just(mobileNumber));
        when(hasSignupServiceClient.generateAadharOtp(aadharOtpRequest)).thenReturn(Mono.just(aadharOtpResponse));

        StepVerifier.create(hasSignupService.generateAadharOtp(aadharOtpRequest,token))
                .expectErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().is4xxClientError())
                .verify();
    }

    @Test
    public void shouldThrowErrorWhenClientGivesError() {
        var token = string();
        var aadharOtpRequest = GenerateAadharOtpRequest.builder().aadhaar("123456789012").build();
        var sessionId = string();
        var mobileNumber = string();

        when(signupService.getSessionId(token)).thenReturn(sessionId);
        when(signupService.getMobileNumber(sessionId)).thenReturn(Mono.just(mobileNumber));
        when(hasSignupServiceClient.generateAadharOtp(aadharOtpRequest)).thenReturn(Mono.error(ClientError.networkServiceCallFailed()));

        StepVerifier.create(hasSignupService.generateAadharOtp(aadharOtpRequest,token))
                .expectErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().is5xxServerError())
                .verify();
    }

//    @Test
//    public void shouldVerifyAadharOTP() {
//        var token = string();
//        var verifyAadharOtpRequest = VerifyAadharOtpRequest.builder().txnId("testTxnId").otp("666666").build();
//        var sessionId = string();
//        var mobileNumber = string();
//        var hasUser = HealthAccountUser.builder()
//                .healthId(string())
//                .token(token)
//                .dayOfBirth(1).build();
//
//        when(signupService.getSessionId(anyString())).thenReturn(sessionId);
//        when(signupService.getMobileNumber(anyString())).thenReturn(Mono.just(mobileNumber));
//        when(dummyHealthAccountService.createHASUser()).thenReturn(hasUser);
//        when(hasSignupServiceClient.verifyAadharOtp(any(VerifyAadharOtpRequest.class))).thenReturn(Mono.just(hasUser));
//        when(hasCache.put(anyString(),anyString())).thenReturn(Mono.empty());
//        when(userRepository.getPatientByHealthId(anyString())).thenReturn(Mono.empty());
//        when(userRepository.save(any(HealthAccountUser.class),anyString())).thenReturn(Mono.empty());
//        when(signupService.removeOf(anyString())).thenReturn(Mono.empty());
//
//        StepVerifier.create(hasSignupService.verifyAadharOtp(verifyAadharOtpRequest,token))
//                .assertNext(response -> {
//                    assertThat(response.getHealthId()).isEqualTo(hasUser.getHealthId());
//                    assertThat(response.getDateOfBirth().getDate()).isEqualTo(hasUser.getDayOfBirth());
//                    assertThat(response.getToken()).isEqualTo(token);
//                })
//                .verifyComplete();
//    }
}