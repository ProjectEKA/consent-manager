package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.OtpServiceClient;
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
    private UserVerificationService userVerificationService;

    private UserConfiguration userConfiguration = new UserConfiguration();

    @Test
    public void shouldReturnUserServiceInstance() {
        assertThat(userConfiguration.userService(
                mockUserRepository,
                otpServiceProperties,
                otpServiceClient,
                userVerificationService))
                .isInstanceOf(UserService.class);
    }
}
