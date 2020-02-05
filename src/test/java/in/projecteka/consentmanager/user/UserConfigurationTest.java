package in.projecteka.consentmanager.user;

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

    private UserConfiguration userConfiguration = new UserConfiguration();


    @Test
    public void shouldReturnUserServiceInstance() {
        assertThat(userConfiguration.userService(mockUserRepository, otpServiceProperties, otpServiceClient))
                .isInstanceOf(UserService.class);
    }
}
