package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.user.model.HASSignupRequest;
import in.projecteka.consentmanager.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class DummyHealthAccountServiceTest {

    @Mock
    UserRepository userRepository;

    DummyHealthAccountService dummyHealthAccountService;

    @BeforeEach
    void setUp() {
        initMocks(this);
        dummyHealthAccountService = new DummyHealthAccountService(userRepository);
    }

    @Test
    public void shouldCreateNewHASUser() {
        var signUpRequest = HASSignupRequest.builder()
                .firstName("Hina")
                .lastName("Patel")
                .middleName("")
                .dayOfBirth(6)
                .monthOfBirth(12)
                .yearOfBirth(1960)
                .gender("F")
                .token("")
                .txnId("")
                .build();

        when(userRepository.getExistingUser(signUpRequest.getFirstName(), signUpRequest.getLastName(), signUpRequest.getGender()))
                .thenReturn(Mono.empty());


        StepVerifier.create(dummyHealthAccountService.createHASAccount(signUpRequest))
                .assertNext(healthAccountUser -> {
                    assertThat(healthAccountUser.getHealthId()).isNotEmpty();
                    assertThat(healthAccountUser.getToken()).isNotEmpty();
                    assertThat(healthAccountUser.getFirstName()).isEqualTo(signUpRequest.getFirstName());
                    assertThat(healthAccountUser.getLastName()).isEqualTo(signUpRequest.getLastName());
                    assertThat(healthAccountUser.getNewHASUser()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    public void shouldReturnHASUserMatchingWithSignUpRequest() {
        var signUpRequest = HASSignupRequest.builder()
                .firstName("Hina")
                .lastName("Patel")
                .middleName("")
                .dayOfBirth(6)
                .monthOfBirth(12)
                .yearOfBirth(1960)
                .gender("F")
                .token("")
                .txnId("")
                .build();
        User user = TestBuilders.user().build();

        when(userRepository.getExistingUser(signUpRequest.getFirstName(), signUpRequest.getLastName(), signUpRequest.getGender()))
                .thenReturn(Mono.just(user));

        StepVerifier.create(dummyHealthAccountService.createHASAccount(signUpRequest))
                .assertNext(healthAccountUser -> {
                    assertThat(healthAccountUser.getHealthId()).isEqualTo(user.getHealthId());
                    assertThat(healthAccountUser.getToken()).isNotEmpty();
                    assertThat(healthAccountUser.getFirstName()).isEqualTo(user.getName().getFirst());
                    assertThat(healthAccountUser.getLastName()).isEqualTo(user.getName().getLast());
                    assertThat(healthAccountUser.getNewHASUser()).isFalse();
                })
                .verifyComplete();
    }
}