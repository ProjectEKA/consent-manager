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
        when(otpAttemptRepository.getOtpAttempts(phoneNumber, userServiceProperties.getMaxOtpAttempts(),OtpAttempt.Action.REGISTRATION))
                .thenReturn(Mono.just(new ArrayList<>()));
        when(otpAttemptRepository.insert(eq(phoneNumber), eq(false), eq(OtpAttempt.Action.REGISTRATION))).thenReturn(Mono.empty());

        StepVerifier.create(otpAttemptService.validateOTPRequest(phoneNumber,OtpAttempt.Action.REGISTRATION))
                .verifyComplete();
    }

    @Test
    void shouldInsertOTPAttemptIfAnyOfTheAttemptIsBlockedExcludingLatest() {
        var otpAttempts = new ArrayList<OtpAttempt>();
        otpAttempts.add(new OtpAttempt(phoneNumber, false, LocalDateTime.now(),OtpAttempt.Action.REGISTRATION));
        otpAttempts.add(new OtpAttempt(phoneNumber, false, LocalDateTime.now(),OtpAttempt.Action.REGISTRATION));
        otpAttempts.add(new OtpAttempt(phoneNumber, true, LocalDateTime.now(),OtpAttempt.Action.REGISTRATION));
        otpAttempts.add(new OtpAttempt(phoneNumber, false, LocalDateTime.now(),OtpAttempt.Action.REGISTRATION));
        otpAttempts.add(new OtpAttempt(phoneNumber, false, LocalDateTime.now(),OtpAttempt.Action.REGISTRATION));
        when(otpAttemptRepository.getOtpAttempts(phoneNumber, userServiceProperties.getMaxOtpAttempts(),OtpAttempt.Action.REGISTRATION))
                .thenReturn(Mono.just(otpAttempts));
        when(otpAttemptRepository.insert(eq(phoneNumber), eq(false),eq(OtpAttempt.Action.REGISTRATION))).thenReturn(Mono.empty());
        StepVerifier.create(otpAttemptService.validateOTPRequest(phoneNumber,OtpAttempt.Action.REGISTRATION))
                .verifyComplete();
    }

    @Test
    void shouldInsertOTPAttemptWhenLatestAttemptIsBlockedAndBlockingTimeIsPassed() {
        var otpAttempts = new ArrayList<OtpAttempt>();
        var currentTimestamp = LocalDateTime.now(ZoneOffset.UTC);
        otpAttempts.add(new OtpAttempt(phoneNumber, true, currentTimestamp.minusMinutes(5),OtpAttempt.Action.REGISTRATION));
        otpAttempts.add(new OtpAttempt(phoneNumber, false, currentTimestamp.minusMinutes(6),OtpAttempt.Action.REGISTRATION));
        otpAttempts.add(new OtpAttempt(phoneNumber, false, currentTimestamp.minusMinutes(7),OtpAttempt.Action.REGISTRATION));
        otpAttempts.add(new OtpAttempt(phoneNumber, false, currentTimestamp.minusMinutes(8),OtpAttempt.Action.REGISTRATION));
        otpAttempts.add(new OtpAttempt(phoneNumber, false, currentTimestamp.minusMinutes(9),OtpAttempt.Action.REGISTRATION));
        when(otpAttemptRepository.getOtpAttempts(phoneNumber, userServiceProperties.getMaxOtpAttempts(),OtpAttempt.Action.REGISTRATION))
                .thenReturn(Mono.just(otpAttempts));
        when(otpAttemptRepository.insert(eq(phoneNumber), eq(false),eq(OtpAttempt.Action.REGISTRATION))).thenReturn(Mono.empty());
        StepVerifier.create(otpAttemptService.validateOTPRequest(phoneNumber,OtpAttempt.Action.REGISTRATION))
                .verifyComplete();
    }

    @Test
    void shouldInsertOTPAttemptWhenMaxOTPAttemptsLimitIsNotReached() {
        var otpAttempts = new ArrayList<OtpAttempt>();
        var currentTimestamp = LocalDateTime.now(ZoneOffset.UTC);
        otpAttempts.add(new OtpAttempt(phoneNumber, false, currentTimestamp.minusMinutes(5),OtpAttempt.Action.REGISTRATION));
        otpAttempts.add(new OtpAttempt(phoneNumber, false, currentTimestamp.minusMinutes(6),OtpAttempt.Action.REGISTRATION));
        otpAttempts.add(new OtpAttempt(phoneNumber, false, currentTimestamp.minusMinutes(9),OtpAttempt.Action.REGISTRATION));
        otpAttempts.add(new OtpAttempt(phoneNumber, false, currentTimestamp.minusMinutes(12),OtpAttempt.Action.REGISTRATION));
        otpAttempts.add(new OtpAttempt(phoneNumber, false, currentTimestamp.minusMinutes(17),OtpAttempt.Action.REGISTRATION));
        when(otpAttemptRepository.getOtpAttempts(phoneNumber, userServiceProperties.getMaxOtpAttempts(),OtpAttempt.Action.REGISTRATION))
                .thenReturn(Mono.just(otpAttempts));
        when(otpAttemptRepository.insert(eq(phoneNumber), eq(false),eq(OtpAttempt.Action.REGISTRATION))).thenReturn(Mono.empty());
        StepVerifier.create(otpAttemptService.validateOTPRequest(phoneNumber,OtpAttempt.Action.REGISTRATION))
                .verifyComplete();
    }

    @Test
    void shouldThrowLimitExceededErrorWhenBlockingTimeIsNotPassed() {
        var otpAttempts = new ArrayList<OtpAttempt>();
        var currentTimestamp = LocalDateTime.now(ZoneOffset.UTC);
        otpAttempts.add(new OtpAttempt(phoneNumber, true, currentTimestamp.minusMinutes(1),OtpAttempt.Action.REGISTRATION));
        otpAttempts.add(new OtpAttempt(phoneNumber, false, currentTimestamp.minusMinutes(2),OtpAttempt.Action.REGISTRATION));
        otpAttempts.add(new OtpAttempt(phoneNumber, false, currentTimestamp.minusMinutes(3),OtpAttempt.Action.REGISTRATION));
        otpAttempts.add(new OtpAttempt(phoneNumber, false, currentTimestamp.minusMinutes(4),OtpAttempt.Action.REGISTRATION));
        otpAttempts.add(new OtpAttempt(phoneNumber, false, currentTimestamp.minusMinutes(5),OtpAttempt.Action.REGISTRATION));
        when(otpAttemptRepository.getOtpAttempts(phoneNumber, userServiceProperties.getMaxOtpAttempts(),OtpAttempt.Action.REGISTRATION))
                .thenReturn(Mono.just(otpAttempts));
        StepVerifier.create(otpAttemptService.validateOTPRequest(phoneNumber,OtpAttempt.Action.REGISTRATION))
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
        otpAttempts.add(new OtpAttempt(phoneNumber, false, currentTimestamp.minusMinutes(1),OtpAttempt.Action.REGISTRATION));
        otpAttempts.add(new OtpAttempt(phoneNumber, false, currentTimestamp.minusMinutes(2),OtpAttempt.Action.REGISTRATION));
        otpAttempts.add(new OtpAttempt(phoneNumber, false, currentTimestamp.minusMinutes(3),OtpAttempt.Action.REGISTRATION));
        otpAttempts.add(new OtpAttempt(phoneNumber, false, currentTimestamp.minusMinutes(4),OtpAttempt.Action.REGISTRATION));
        otpAttempts.add(new OtpAttempt(phoneNumber, false, currentTimestamp.minusMinutes(5),OtpAttempt.Action.REGISTRATION));
        when(otpAttemptRepository.getOtpAttempts(phoneNumber, userServiceProperties.getMaxOtpAttempts(),OtpAttempt.Action.REGISTRATION))
                .thenReturn(Mono.just(otpAttempts));
        when(otpAttemptRepository.insert(eq(phoneNumber), eq(true),eq(OtpAttempt.Action.REGISTRATION))).thenReturn(Mono.empty());
        StepVerifier.create(otpAttemptService.validateOTPRequest(phoneNumber,OtpAttempt.Action.REGISTRATION))
                .expectErrorMatches(throwable -> throwable instanceof ClientError && ((ClientError) throwable)
                        .getError()
                        .getError()
                        .getCode()
                        .equals(ErrorCode.OTP_REQUEST_LIMIT_EXCEEDED))
                .verify();
    }
}