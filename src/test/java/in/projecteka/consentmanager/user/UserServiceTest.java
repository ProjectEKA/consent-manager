package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.user.exception.InvalidRequestException;
import in.projecteka.consentmanager.user.model.DeviceIdentifier;
import in.projecteka.consentmanager.user.model.OtpVerification;
import in.projecteka.consentmanager.user.model.TemporarySession;
import in.projecteka.consentmanager.user.model.Token;
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
        userService = new UserService(userRepository, otpServiceProperties, otpServiceClient);
        when(otpServiceClient.sendOtpTo(any())).thenReturn(Mono.just(
                new TemporarySession("SOME_SESSION_ID")
        ));
        when(otpServiceClient.permitOtp(any())).thenReturn(Mono.just(new Token("SOME_TEMPORARY_TOKEN")));
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

    @Test
    public void shouldReturnTokenReceivedFromClient() {
        OtpVerification otpVerification = new OtpVerification("SOME_SESSION_ID", "1234");
        StepVerifier.create(userService.permitOtp(otpVerification))
                .assertNext(response -> {
                    assertThat(response.getTemporaryToken()).isEqualTo("SOME_TEMPORARY_TOKEN");
                });
    }

    @Test
    public void shouldThrowInvalidRequestExceptionForInvalidOtpValue() {
        OtpVerification otpVerification = new OtpVerification("SOME_SESSION_ID", "");
        Assertions.assertThrows(InvalidRequestException.class, () -> {
            userService.permitOtp(otpVerification);
        });
    }

    @Test
    public void shouldThrowInvalidRequestExceptionForInvalidOtpSessionId() {
        OtpVerification otpVerification = new OtpVerification("", "1234");
        Assertions.assertThrows(InvalidRequestException.class, () -> {
            userService.permitOtp(otpVerification);
        });
    }

}
