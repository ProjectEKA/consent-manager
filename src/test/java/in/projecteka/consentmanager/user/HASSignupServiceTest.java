package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.HASSignupServiceClient;
import in.projecteka.consentmanager.clients.IdentityServiceClient;
import in.projecteka.consentmanager.clients.OtpServiceClient;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.clients.model.OtpRequest;
import in.projecteka.consentmanager.clients.model.Session;
import in.projecteka.consentmanager.clients.properties.OtpServiceProperties;
import in.projecteka.consentmanager.user.model.DateOfBirth;
import in.projecteka.consentmanager.user.model.Gender;
import in.projecteka.consentmanager.user.model.HASSignupRequest;
import in.projecteka.consentmanager.user.model.HealthAccountUser;
import in.projecteka.consentmanager.user.model.PatientName;
import in.projecteka.consentmanager.user.model.PatientResponse;
import in.projecteka.consentmanager.user.model.SignUpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import static in.projecteka.consentmanager.user.TestBuilders.string;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class HASSignupServiceTest {

//    @Captor
//    private ArgumentCaptor<OtpRequest> otpRequestArgumentCaptor;
//
//    @Captor
//    private ArgumentCaptor<String> sessionCaptor;
//
//    @Mock
//    private UserRepository userRepository;
//
//    @Mock
//    private OtpServiceClient otpServiceClient;
//
//    @Mock
//    private SignUpService signupService;
//
//    @Mock
//    private OtpAttemptService otpAttemptService;
//
//    @Mock
//    private LockedUserService lockedUserService;
//
//    @Mock
//    private IdentityServiceClient identityServiceClient;
//
//    @Mock
//    private HASSignupServiceClient hasSignupServiceClient;
//
//    @Mock
//    private TokenService tokenService;
//
//    @Mock
//    private UserServiceProperties properties;
//
//    @Captor
//    private ArgumentCaptor<ClientRequest> captor;
//
//    @Mock
//    private ExchangeFunction exchangeFunction;
//
//    @Mock
//    private Logger logger;
//
//    @Mock
//    private UserServiceClient userServiceClient;
//
//    @Captor
//    private ArgumentCaptor<PatientResponse> patientResponse;
//
//    private UserService userService;
//
//    private HASSignupService hasSignupService;
//
//    @BeforeEach
//    public void setUp() {
//        MockitoAnnotations.initMocks(this);
//
//        hasSignupService = new HASSignupService(
//                hasSignupServiceClient,
//                userRepository,
//                signupService);
//    }
//
//    @Test
//    public void shouldCreateUser() {
//        var signUpRequest = SignUpRequest.builder()
//                .name(PatientName.builder().first("hina").middle("").last("patel").build())
//                .dateOfBirth(DateOfBirth.builder().date(1).month(1).year(2020).build())
//                .gender(Gender.F)
//                .build();
//
//        var hasUser = HealthAccountUser.builder()
//                .firstName("hina")
//                .middleName("")
//                .lastName("patel")
//                .dayOfBirth(1)
//                .monthOfBirth(1)
//                .yearOfBirth(2020)
//                .healthId("tempId")
//                .name("hina patel")
//                .gender("F")
//                .token("tempToken")
//                .build();
//
//        var txnId = string();
//        var token = string();
//        var mobileNumber = string();
//
//        when(hasSignupServiceClient.createHASAccount(any(HASSignupRequest.class))).thenReturn(Mono.just(hasUser));
//
//        when(signupService.getMobileNumber(txnId)).thenReturn(Mono.just(mobileNumber));
//        when(userRepository.save(any(),anyString())).thenReturn(Mono.empty());
//        when(signupService.removeOf(anyString())).thenReturn(Mono.empty());
//
//        StepVerifier.create(hasSignupService.createHASAccount(signUpRequest,token, txnId))
//                .assertNext(res -> {
//                    assertThat(res.getHealthId()).isEqualTo("tempId");
//                    assertThat(res.getToken()).isEqualTo("tempToken");
//                })
//                .verifyComplete();
//    }

}