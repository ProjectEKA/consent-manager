package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.HASSignupServiceClient;
import in.projecteka.consentmanager.clients.IdentityServiceClient;
import in.projecteka.consentmanager.clients.model.KeycloakUser;
import in.projecteka.consentmanager.clients.model.Session;
import in.projecteka.consentmanager.user.model.DateOfBirth;
import in.projecteka.consentmanager.user.model.Gender;
import in.projecteka.consentmanager.user.model.HASSignupRequest;
import in.projecteka.consentmanager.user.model.HealthAccountUser;
import in.projecteka.consentmanager.user.model.PatientName;
import in.projecteka.consentmanager.user.model.SessionRequest;
import in.projecteka.consentmanager.user.model.SignUpRequest;
import in.projecteka.consentmanager.user.model.UpdateHASUserRequest;
import in.projecteka.consentmanager.user.model.UpdateLoginDetailsRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static in.projecteka.consentmanager.user.TestBuilders.session;
import static in.projecteka.consentmanager.user.TestBuilders.string;
import static in.projecteka.consentmanager.user.TestBuilders.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

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
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    private HASSignupService hasSignupService;

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
                bCryptPasswordEncoder);
    }

    @Test
    public void shouldCreateUser() {
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

        var txnId = string();
        var token = string();
        var mobileNumber = string();

        when(hasSignupServiceClient.createHASAccount(any(HASSignupRequest.class))).thenReturn(Mono.just(hasUser));

        when(signupService.getMobileNumber(txnId)).thenReturn(Mono.just(mobileNumber));
        when(userRepository.save(any(),anyString())).thenReturn(Mono.empty());
        when(signupService.removeOf(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(hasSignupService.createHASAccount(signUpRequest,token, txnId))
                .assertNext(res -> {
                    assertThat(res.getHealthId()).isEqualTo("tempId");
                    assertThat(res.getToken()).isEqualTo("tempToken");
                })
                .verifyComplete();
    }

    @Test
    public void shouldThrowErrorWhenUnableToCreateUserOnHAS() {
        var signUpRequest = SignUpRequest.builder()
                .name(PatientName.builder().first("hina").middle("").last("patel").build())
                .dateOfBirth(DateOfBirth.builder().date(1).month(1).year(2020).build())
                .gender(Gender.F)
                .build();

        var txnId = string();
        var token = string();

        when(hasSignupServiceClient.createHASAccount(any(HASSignupRequest.class)))
                .thenReturn(Mono.error(ClientError.networkServiceCallFailed()));

        StepVerifier.create(hasSignupService.createHASAccount(signUpRequest,token, txnId))
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

        var txnId = string();
        var token = string();

        when(hasSignupServiceClient.createHASAccount(any(HASSignupRequest.class)))
                .thenReturn(Mono.error(ClientError.unAuthorized()));

        StepVerifier.create(hasSignupService.createHASAccount(signUpRequest,token, txnId))
                .expectErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().is4xxClientError())
                .verify();
    }

    @Test
    public void shouldUpdateLoginDetails() {
        var updateLoginRequestDetails = UpdateLoginDetailsRequest.builder().cmId("hinapatel456@ncg")
                .healthId("12345-12345-12345")
                .password("Test@1243")
                .build();
        var token = string();
        var patientName = PatientName.builder().first("hina").middle("").last("patel").build();
        var session = session().build();

        when(userRepository.userWith(updateLoginRequestDetails.getCmId())).thenReturn(Mono.empty());
        when(hasSignupServiceClient.updateHASAccount(any(UpdateHASUserRequest.class))).thenReturn(Mono.empty());
        when(userRepository.updateCMId(anyString(),anyString()))
                .thenReturn(Mono.empty());
        when(userRepository.getNameByHealthId(updateLoginRequestDetails.getHealthId()))
                .thenReturn(Mono.just(patientName));
        when(tokenService.tokenForAdmin()).thenReturn(Mono.just(session));
        when(identityServiceClient.createUser(any(Session.class),any(KeycloakUser.class))).thenReturn(Mono.empty());
        when(sessionService.forNew(any(SessionRequest.class))).thenReturn(Mono.just(session));
        when(bCryptPasswordEncoder.encode(updateLoginRequestDetails.getPassword())).thenReturn(anyString());

        StepVerifier.create(hasSignupService.updateHASLoginDetails(updateLoginRequestDetails,token))
                .assertNext(res -> {
                    assertThat(res.getAccessToken()).isEqualTo(session.getAccessToken());
                    assertThat(res.getTokenType()).isEqualTo(session.getTokenType());
                })
                .verifyComplete();
    }

    @Test
    public void shouldThrowErrorWhenUserAlreadyExists() {
        var updateLoginRequestDetails = UpdateLoginDetailsRequest.builder().cmId("hinapatel456@ncg")
                .healthId("12345-12345-12345")
                .password("Test@1243")
                .build();
        var token = string();
        var user = user().build();

        when(userRepository.userWith(anyString()))
                .thenReturn(Mono.just(user));

        StepVerifier.create(hasSignupService.updateHASLoginDetails(updateLoginRequestDetails,token))
                .expectErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().is4xxClientError())
                .verify();
    }

    @Test
    public void shouldThrowErrorWhenUnableToUpdatePasswordOnHAS() {
        var updateLoginRequestDetails = UpdateLoginDetailsRequest.builder().cmId("hinapatel456@ncg")
                .healthId("12345-12345-12345")
                .password("Test@1243")
                .build();
        var token = string();

        when(userRepository.userWith(updateLoginRequestDetails.getCmId())).thenReturn(Mono.empty());
        when(hasSignupServiceClient.updateHASAccount(any(UpdateHASUserRequest.class)))
                .thenReturn(Mono.error(ClientError.networkServiceCallFailed()));

        StepVerifier.create(hasSignupService.updateHASLoginDetails(updateLoginRequestDetails,token))
                .expectErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().is5xxServerError())
                .verify();
    }

    @Test
    public void shouldThrowErrorWhenHealthIdNotFound() {
        var updateLoginRequestDetails = UpdateLoginDetailsRequest.builder().cmId("hinapatel456@ncg")
                .healthId("12345-12345-12345")
                .password("Test@1243")
                .build();
        var token = string();

        when(userRepository.userWith(updateLoginRequestDetails.getCmId())).thenReturn(Mono.empty());
        when(hasSignupServiceClient.updateHASAccount(any(UpdateHASUserRequest.class))).thenReturn(Mono.empty());
        when(userRepository.updateCMId(anyString(),anyString()))
                .thenReturn(Mono.empty());
        when(userRepository.getNameByHealthId(updateLoginRequestDetails.getHealthId()))
                .thenReturn(Mono.empty());

        StepVerifier.create(hasSignupService.updateHASLoginDetails(updateLoginRequestDetails,token))
                .expectErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().is4xxClientError())
                .verify();
    }

    @Test
    public void shouldThrowErrorWhenCMIdAlreadyExistsInKeycloak() {
        var updateLoginRequestDetails = UpdateLoginDetailsRequest.builder().cmId("hinapatel456@ncg")
                .healthId("12345-12345-12345")
                .password("Test@1243")
                .build();
        var token = string();
        var patientName = PatientName.builder().first("hina").middle("").last("patel").build();
        var session = session().build();


        when(userRepository.userWith(updateLoginRequestDetails.getCmId())).thenReturn(Mono.empty());
        when(hasSignupServiceClient.updateHASAccount(any(UpdateHASUserRequest.class))).thenReturn(Mono.empty());
        when(userRepository.updateCMId(anyString(),anyString()))
                .thenReturn(Mono.empty());
        when(userRepository.getNameByHealthId(anyString()))
                .thenReturn(Mono.just(patientName));
        when(tokenService.tokenForAdmin()).thenReturn(Mono.just(session));
        when(identityServiceClient.createUser(any(Session.class),any(KeycloakUser.class)))
                .thenReturn(Mono.error(ClientError.networkServiceCallFailed()));

        StepVerifier.create(hasSignupService.updateHASLoginDetails(updateLoginRequestDetails,token))
                .expectErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().is4xxClientError())
                .verify();
    }

    @Test
    public void shouldThrowErrorWhenUserNameOrPasswordIsInvalid() {
        var updateLoginRequestDetails = UpdateLoginDetailsRequest.builder().cmId("hinapatel456@ncg")
                .healthId("12345-12345-12345")
                .password("Test@1243")
                .build();
        var token = string();
        var patientName = PatientName.builder().first("hina").middle("").last("patel").build();
        var session = session().build();


        when(userRepository.userWith(updateLoginRequestDetails.getCmId())).thenReturn(Mono.empty());
        when(hasSignupServiceClient.updateHASAccount(any(UpdateHASUserRequest.class))).thenReturn(Mono.empty());
        when(userRepository.updateCMId(anyString(),anyString()))
                .thenReturn(Mono.empty());
        when(userRepository.getNameByHealthId(updateLoginRequestDetails.getHealthId()))
                .thenReturn(Mono.just(patientName));
        when(tokenService.tokenForAdmin()).thenReturn(Mono.just(session));
        when(identityServiceClient.createUser(any(Session.class),any(KeycloakUser.class)))
                .thenReturn(Mono.empty());
        when(sessionService.forNew(any(SessionRequest.class))).thenReturn(Mono.error(ClientError.invalidUserNameOrPassword()));

        StepVerifier.create(hasSignupService.updateHASLoginDetails(updateLoginRequestDetails,token))
                .expectErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().is4xxClientError())
                .verify();
    }
}