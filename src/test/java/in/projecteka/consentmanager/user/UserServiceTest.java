package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.user.exception.InvalidRequestException;
import in.projecteka.consentmanager.user.model.DeviceIdentifier;
import in.projecteka.consentmanager.user.model.TemporarySession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OtpServiceClient otpServiceClient;
    private UserService userService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        OtpServiceProperties otpServiceProperties = new OtpServiceProperties("", Arrays.asList("MOBILE"));
        userService = new UserService(userRepository,otpServiceProperties,otpServiceClient);
        when(otpServiceClient.sendOtpTo(any())).thenReturn(Mono.just(
                new TemporarySession("SOME_SESSION_ID")
        ));
    }

    @Test
    public void shouldReturnTemporarySessionReceivedFromClient() {
        DeviceIdentifier deviceIdentifier = new DeviceIdentifier("MOBILE", "1234567891");

        StepVerifier.create(userService.sendOtp(deviceIdentifier))
                .assertNext(response -> {
                    assertThat(response.getSessionId()).isEqualTo("SOME_SESSION_ID");
                });
    }

    @Test
    public void shouldThrowInvalidRequestExceptionForInvalidDeviceType() {
        DeviceIdentifier deviceIdentifier = new DeviceIdentifier("INVALID_DEVICE", "1234567891");

        Assertions.assertThrows(InvalidRequestException.class, () -> {
            userService.sendOtp(deviceIdentifier);
        });
    }

}
