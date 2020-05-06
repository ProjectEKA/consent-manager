package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.model.ErrorCode;
import in.projecteka.consentmanager.user.model.OtpAttempt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class OtpAttemptServiceTest {

    @Mock
    OtpAttemptRepository otpAttemptRepository;


    private UserServiceProperties userServiceProperties;

    private OtpAttemptService otpAttemptService;

    private String phoneNumber;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        userServiceProperties = new UserServiceProperties(
                "",
                0,
                0,
                0,
                "",
                5,
                10,
                2);
        otpAttemptService = new OtpAttemptService(otpAttemptRepository, userServiceProperties);
        phoneNumber = "+91-6666666666";
    }

    @Test
    public void shouldInsertOTPAttemptForFirstNAttempts() { // where N is maxOtpAttempts
        when(otpAttemptRepository.getOtpAttempts(phoneNumber, userServiceProperties.getMaxOtpAttempts()))
                .thenReturn(Mono.just(new ArrayList<>()));
        when(otpAttemptRepository.insert(eq(phoneNumber), eq(false))).thenReturn(Mono.empty());

        StepVerifier.create(otpAttemptService.validateOTPRequest(phoneNumber))
                .verifyComplete();
    }

    @Test
    void shouldInsertOTPAttemptIfAnyOfTheAttemptIsBlockedExcludingLatest() {
        var otpAttempts = new ArrayList<OtpAttempt>();
        otpAttempts.add(new OtpAttempt(phoneNumber, false, LocalDateTime.now()));
        otpAttempts.add(new OtpAttempt(phoneNumber, false, LocalDateTime.now()));
        otpAttempts.add(new OtpAttempt(phoneNumber, true, LocalDateTime.now()));
        otpAttempts.add(new OtpAttempt(phoneNumber, false, LocalDateTime.now()));
        otpAttempts.add(new OtpAttempt(phoneNumber, false, LocalDateTime.now()));
        when(otpAttemptRepository.getOtpAttempts(phoneNumber, userServiceProperties.getMaxOtpAttempts()))
                .thenReturn(Mono.just(otpAttempts));
        when(otpAttemptRepository.insert(eq(phoneNumber), eq(false))).thenReturn(Mono.empty());
        StepVerifier.create(otpAttemptService.validateOTPRequest(phoneNumber))
                .verifyComplete();
    }

    @Test
    void shouldInsertOTPAttemptWhenLatestAttemptIsBlockedAndBlockingTimeIsPassed() {
        var otpAttempts = new ArrayList<OtpAttempt>();
        var currentTimestamp = LocalDateTime.now(ZoneOffset.UTC);
        otpAttempts.add(new OtpAttempt(phoneNumber, true, currentTimestamp.minusMinutes(5)));
        otpAttempts.add(new OtpAttempt(phoneNumber, false, currentTimestamp.minusMinutes(6)));
        otpAttempts.add(new OtpAttempt(phoneNumber, false, currentTimestamp.minusMinutes(7)));
        otpAttempts.add(new OtpAttempt(phoneNumber, false, currentTimestamp.minusMinutes(8)));
        otpAttempts.add(new OtpAttempt(phoneNumber, false, currentTimestamp.minusMinutes(9)));
        when(otpAttemptRepository.getOtpAttempts(phoneNumber, userServiceProperties.getMaxOtpAttempts()))
                .thenReturn(Mono.just(otpAttempts));
        when(otpAttemptRepository.insert(eq(phoneNumber), eq(false))).thenReturn(Mono.empty());
        StepVerifier.create(otpAttemptService.validateOTPRequest(phoneNumber))
                .verifyComplete();
    }

    @Test
    void shouldInsertOTPAttemptWhenMaxOTPAttemptsLimitIsNotReached() {
        var otpAttempts = new ArrayList<OtpAttempt>();
        var currentTimestamp = LocalDateTime.now(ZoneOffset.UTC);
        otpAttempts.add(new OtpAttempt(phoneNumber, false, currentTimestamp.minusMinutes(5)));
        otpAttempts.add(new OtpAttempt(phoneNumber, false, currentTimestamp.minusMinutes(6)));
        otpAttempts.add(new OtpAttempt(phoneNumber, false, currentTimestamp.minusMinutes(9)));
        otpAttempts.add(new OtpAttempt(phoneNumber, false, currentTimestamp.minusMinutes(12)));
        otpAttempts.add(new OtpAttempt(phoneNumber, false, currentTimestamp.minusMinutes(17)));
        when(otpAttemptRepository.getOtpAttempts(phoneNumber, userServiceProperties.getMaxOtpAttempts()))
                .thenReturn(Mono.just(otpAttempts));
        when(otpAttemptRepository.insert(eq(phoneNumber), eq(false))).thenReturn(Mono.empty());
        StepVerifier.create(otpAttemptService.validateOTPRequest(phoneNumber))
                .verifyComplete();
    }

    @Test
    void shouldThrowLimitExceededErrorWhenBlockingTimeIsNotPassed() {
        var otpAttempts = new ArrayList<OtpAttempt>();
        var currentTimestamp = LocalDateTime.now(ZoneOffset.UTC);
        otpAttempts.add(new OtpAttempt(phoneNumber, true, currentTimestamp.minusMinutes(1)));
        otpAttempts.add(new OtpAttempt(phoneNumber, false, currentTimestamp.minusMinutes(2)));
        otpAttempts.add(new OtpAttempt(phoneNumber, false, currentTimestamp.minusMinutes(3)));
        otpAttempts.add(new OtpAttempt(phoneNumber, false, currentTimestamp.minusMinutes(4)));
        otpAttempts.add(new OtpAttempt(phoneNumber, false, currentTimestamp.minusMinutes(5)));
        when(otpAttemptRepository.getOtpAttempts(phoneNumber, userServiceProperties.getMaxOtpAttempts()))
                .thenReturn(Mono.just(otpAttempts));
        StepVerifier.create(otpAttemptService.validateOTPRequest(phoneNumber))
                .expectErrorMatches(throwable -> throwable instanceof ClientError && ((ClientError) throwable)
                        .getError()
                        .getError()
                        .getCode()
                        .equals(ErrorCode.OTP_REQUEST_LIMIT_EXCEEDED))
                .verify();
    }

    @Test
    void shouldThrowLimitExceededErrorOnExceedingMaxOTPAttempts() {
        var otpAttempts = new ArrayList<OtpAttempt>();
        var currentTimestamp = LocalDateTime.now(ZoneOffset.UTC);
        otpAttempts.add(new OtpAttempt(phoneNumber, false, currentTimestamp.minusMinutes(1)));
        otpAttempts.add(new OtpAttempt(phoneNumber, false, currentTimestamp.minusMinutes(2)));
        otpAttempts.add(new OtpAttempt(phoneNumber, false, currentTimestamp.minusMinutes(3)));
        otpAttempts.add(new OtpAttempt(phoneNumber, false, currentTimestamp.minusMinutes(4)));
        otpAttempts.add(new OtpAttempt(phoneNumber, false, currentTimestamp.minusMinutes(5)));
        when(otpAttemptRepository.getOtpAttempts(phoneNumber, userServiceProperties.getMaxOtpAttempts()))
                .thenReturn(Mono.just(otpAttempts));
        when(otpAttemptRepository.insert(eq(phoneNumber), eq(true))).thenReturn(Mono.empty());
        StepVerifier.create(otpAttemptService.validateOTPRequest(phoneNumber))
                .expectErrorMatches(throwable -> throwable instanceof ClientError && ((ClientError) throwable)
                        .getError()
                        .getError()
                        .getCode()
                        .equals(ErrorCode.OTP_REQUEST_LIMIT_EXCEEDED))
                .verify();
    }
}