package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.IdentityServiceClient;
import in.projecteka.consentmanager.clients.OtpServiceClient;
import in.projecteka.consentmanager.clients.properties.OtpServiceProperties;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;

class UserConfigurationTest {

    @Mock
    private UserRepository mockUserRepository;

    @Mock
    private OtpServiceProperties otpServiceProperties;

    @Mock
    private OtpServiceClient otpServiceClient;

    @Mock
    private SignUpService signupService;

    @Mock
    private IdentityServiceClient identityServiceClient;

    @Mock
    private TokenService tokenService;

    @Mock
    private RegistrationRequestService registrationRequestService;

    @Mock
    private UserServiceProperties properties;

    private final UserConfiguration userConfiguration = new UserConfiguration();

    @Test
    public void shouldReturnUserServiceInstance() {
        assertThat(userConfiguration.userService(
                mockUserRepository,
                otpServiceProperties,
                otpServiceClient,
                signupService,
                identityServiceClient,
                tokenService,
                properties,
                registrationRequestService))
                .isInstanceOf(UserService.class);
    }
}
