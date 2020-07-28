package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.HealthAccountServiceClient;
import in.projecteka.consentmanager.clients.IdentityServiceClient;
import in.projecteka.consentmanager.clients.OtpServiceClient;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.clients.model.DistrictData;
import in.projecteka.consentmanager.clients.model.StateData;
import in.projecteka.consentmanager.clients.properties.HealthAccountServiceProperties;
import in.projecteka.consentmanager.clients.properties.OtpServiceProperties;
import in.projecteka.consentmanager.consent.ConsentServiceProperties;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserConfigurationTest {

    @Mock
    private UserRepository mockUserRepository;

    @Mock
    private OtpServiceProperties otpServiceProperties;

    @Mock
    private OtpServiceClient otpServiceClient;

    @Mock
    private HealthAccountServiceProperties healthAccountServiceProperties;

    @Mock
    private HealthAccountServiceClient healthAccountServiceClient;

    @Mock
    private SignUpService signupService;

    @Mock
    private IdentityServiceClient identityServiceClient;

    @Mock
    private TokenService tokenService;

    @Mock
    private OtpAttemptService otpAttemptService;

    @Mock
    private LockedUserService lockedUserService;

    @Mock
    private UserServiceProperties properties;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private ConsentServiceProperties consentServiceProperties;

    private final UserConfiguration userConfiguration = new UserConfiguration();

    @Mock
    private CacheAdapter<String, List<StateData>> stateCache;

    @Mock
    private CacheAdapter<String, List<DistrictData>> districtCache;

    @Test
    public void shouldReturnUserServiceInstance() {
        assertThat(userConfiguration.userService(
                mockUserRepository,
                otpServiceProperties,
                otpServiceClient,
                healthAccountServiceProperties,
                healthAccountServiceClient,
                signupService,
                identityServiceClient,
                tokenService,
                properties,
                otpAttemptService,
                lockedUserService,
                userServiceClient,
                stateCache,
                districtCache,
                consentServiceProperties))
                .isInstanceOf(UserService.class);
    }
}
