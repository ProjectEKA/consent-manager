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

    private String identifierType;

    private String identifierValue;

    private String cmId;

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
                2,
                5);
        otpAttemptService = new OtpAttemptService(otpAttemptRepository, userServiceProperties);
        identifierValue = "+91-6666666666";
        cmId = "";
        identifierType = "mobile";
    }

    @Test
    public void shouldInsertOTPAttemptForFirstNAttempts() { // where N is maxOtpAttempts
        when(otpAttemptRepository.getOtpAttempts(cmId, identifierType, identifierValue, userServiceProperties.getMaxOtpAttempts(), OtpAttempt.Action.OTP_REQUEST_REGISTRATION))
                .thenReturn(Mono.just(new ArrayList<>()));
        when(otpAttemptRepository.insert(eq(cmId), eq(identifierType), eq(identifierValue), eq(OtpAttempt.AttemptStatus.SUCCESS), eq(OtpAttempt.Action.OTP_REQUEST_REGISTRATION))).thenReturn(Mono.empty());

        StepVerifier.create(otpAttemptService.validateOTPRequest(identifierType, identifierValue, OtpAttempt.Action.OTP_REQUEST_REGISTRATION))
                .verifyComplete();
    }

    @Test
    void shouldInsertOTPAttemptIfAnyOfTheAttemptIsBlockedExcludingLatest() {
        var otpAttempts = new ArrayList<OtpAttempt>();
        otpAttempts.add(new OtpAttempt(identifierType, identifierValue, OtpAttempt.AttemptStatus.SUCCESS, LocalDateTime.now(), OtpAttempt.Action.OTP_REQUEST_REGISTRATION, cmId));
        otpAttempts.add(new OtpAttempt(identifierType, identifierValue, OtpAttempt.AttemptStatus.SUCCESS, LocalDateTime.now(), OtpAttempt.Action.OTP_REQUEST_REGISTRATION, cmId));
        otpAttempts.add(new OtpAttempt(identifierType, identifierValue, OtpAttempt.AttemptStatus.FAILURE, LocalDateTime.now(), OtpAttempt.Action.OTP_REQUEST_REGISTRATION, cmId));
        otpAttempts.add(new OtpAttempt(identifierType, identifierValue, OtpAttempt.AttemptStatus.SUCCESS, LocalDateTime.now(), OtpAttempt.Action.OTP_REQUEST_REGISTRATION, cmId));
        otpAttempts.add(new OtpAttempt(identifierType, identifierValue, OtpAttempt.AttemptStatus.SUCCESS, LocalDateTime.now(), OtpAttempt.Action.OTP_REQUEST_REGISTRATION, cmId));
        when(otpAttemptRepository.getOtpAttempts(cmId, identifierType, identifierValue, userServiceProperties.getMaxOtpAttempts(), OtpAttempt.Action.OTP_REQUEST_REGISTRATION))
                .thenReturn(Mono.just(otpAttempts));
        when(otpAttemptRepository.insert(eq(cmId), eq(identifierType), eq(identifierValue), eq(OtpAttempt.AttemptStatus.SUCCESS), eq(OtpAttempt.Action.OTP_REQUEST_REGISTRATION))).thenReturn(Mono.empty());
        StepVerifier.create(otpAttemptService.validateOTPRequest(identifierType, identifierValue, OtpAttempt.Action.OTP_REQUEST_REGISTRATION))
                .verifyComplete();
    }

    @Test
    void shouldInsertOTPAttemptWhenLatestAttemptIsBlockedAndBlockingTimeIsPassed() {
        var otpAttempts = new ArrayList<OtpAttempt>();
        var currentTimestamp = LocalDateTime.now(ZoneOffset.UTC);
        otpAttempts.add(new OtpAttempt(identifierType, identifierValue, OtpAttempt.AttemptStatus.FAILURE, currentTimestamp.minusMinutes(5), OtpAttempt.Action.OTP_REQUEST_REGISTRATION, cmId));
        otpAttempts.add(new OtpAttempt(identifierType, identifierValue, OtpAttempt.AttemptStatus.SUCCESS, currentTimestamp.minusMinutes(6), OtpAttempt.Action.OTP_REQUEST_REGISTRATION, cmId));
        otpAttempts.add(new OtpAttempt(identifierType, identifierValue, OtpAttempt.AttemptStatus.SUCCESS, currentTimestamp.minusMinutes(7), OtpAttempt.Action.OTP_REQUEST_REGISTRATION, cmId));
        otpAttempts.add(new OtpAttempt(identifierType, identifierValue, OtpAttempt.AttemptStatus.SUCCESS, currentTimestamp.minusMinutes(8), OtpAttempt.Action.OTP_REQUEST_REGISTRATION, cmId));
        otpAttempts.add(new OtpAttempt(identifierType, identifierValue, OtpAttempt.AttemptStatus.SUCCESS, currentTimestamp.minusMinutes(9), OtpAttempt.Action.OTP_REQUEST_REGISTRATION, cmId));
        when(otpAttemptRepository.getOtpAttempts(cmId, identifierType, identifierValue, userServiceProperties.getMaxOtpAttempts(), OtpAttempt.Action.OTP_REQUEST_REGISTRATION))
                .thenReturn(Mono.just(otpAttempts));
        when(otpAttemptRepository.insert(eq(cmId), eq(identifierType), eq(identifierValue), eq(OtpAttempt.AttemptStatus.SUCCESS), eq(OtpAttempt.Action.OTP_REQUEST_REGISTRATION))).thenReturn(Mono.empty());
        StepVerifier.create(otpAttemptService.validateOTPRequest(identifierType, identifierValue, OtpAttempt.Action.OTP_REQUEST_REGISTRATION))
                .verifyComplete();
    }

    @Test
    void shouldInsertOTPAttemptWhenMaxOTPAttemptsLimitIsNotReached() {
        var otpAttempts = new ArrayList<OtpAttempt>();
        var currentTimestamp = LocalDateTime.now(ZoneOffset.UTC);
        otpAttempts.add(new OtpAttempt(identifierType, identifierValue, OtpAttempt.AttemptStatus.SUCCESS, currentTimestamp.minusMinutes(5), OtpAttempt.Action.OTP_REQUEST_REGISTRATION, cmId));
        otpAttempts.add(new OtpAttempt(identifierType, identifierValue, OtpAttempt.AttemptStatus.SUCCESS, currentTimestamp.minusMinutes(6), OtpAttempt.Action.OTP_REQUEST_REGISTRATION, cmId));
        otpAttempts.add(new OtpAttempt(identifierType, identifierValue, OtpAttempt.AttemptStatus.SUCCESS, currentTimestamp.minusMinutes(9), OtpAttempt.Action.OTP_REQUEST_REGISTRATION, cmId));
        otpAttempts.add(new OtpAttempt(identifierType, identifierValue, OtpAttempt.AttemptStatus.SUCCESS, currentTimestamp.minusMinutes(12), OtpAttempt.Action.OTP_REQUEST_REGISTRATION, cmId));
        otpAttempts.add(new OtpAttempt(identifierType, identifierValue, OtpAttempt.AttemptStatus.SUCCESS, currentTimestamp.minusMinutes(17), OtpAttempt.Action.OTP_REQUEST_REGISTRATION, cmId));
        when(otpAttemptRepository.getOtpAttempts(cmId, identifierType, identifierValue, userServiceProperties.getMaxOtpAttempts(), OtpAttempt.Action.OTP_REQUEST_REGISTRATION))
                .thenReturn(Mono.just(otpAttempts));
        when(otpAttemptRepository.insert(eq(cmId), eq(identifierType), eq(identifierValue), eq(OtpAttempt.AttemptStatus.SUCCESS), eq(OtpAttempt.Action.OTP_REQUEST_REGISTRATION))).thenReturn(Mono.empty());
        StepVerifier.create(otpAttemptService.validateOTPRequest(identifierType, identifierValue, OtpAttempt.Action.OTP_REQUEST_REGISTRATION))
                .verifyComplete();
    }

    @Test
    void shouldThrowLimitExceededErrorWhenBlockingTimeIsNotPassed() {
        var otpAttempts = new ArrayList<OtpAttempt>();
        var currentTimestamp = LocalDateTime.now(ZoneOffset.UTC);
        otpAttempts.add(new OtpAttempt(identifierType, identifierValue, OtpAttempt.AttemptStatus.FAILURE, currentTimestamp.minusMinutes(1), OtpAttempt.Action.OTP_REQUEST_REGISTRATION, cmId));
        otpAttempts.add(new OtpAttempt(identifierType, identifierValue, OtpAttempt.AttemptStatus.SUCCESS, currentTimestamp.minusMinutes(2), OtpAttempt.Action.OTP_REQUEST_REGISTRATION, cmId));
        otpAttempts.add(new OtpAttempt(identifierType, identifierValue, OtpAttempt.AttemptStatus.SUCCESS, currentTimestamp.minusMinutes(3), OtpAttempt.Action.OTP_REQUEST_REGISTRATION, cmId));
        otpAttempts.add(new OtpAttempt(identifierType, identifierValue, OtpAttempt.AttemptStatus.SUCCESS, currentTimestamp.minusMinutes(4), OtpAttempt.Action.OTP_REQUEST_REGISTRATION, cmId));
        otpAttempts.add(new OtpAttempt(identifierType, identifierValue, OtpAttempt.AttemptStatus.SUCCESS, currentTimestamp.minusMinutes(5), OtpAttempt.Action.OTP_REQUEST_REGISTRATION, cmId));
        when(otpAttemptRepository.getOtpAttempts(cmId, identifierType, identifierValue, userServiceProperties.getMaxOtpAttempts(), OtpAttempt.Action.OTP_REQUEST_REGISTRATION))
                .thenReturn(Mono.just(otpAttempts));
        StepVerifier.create(otpAttemptService.validateOTPRequest(identifierType, identifierValue, OtpAttempt.Action.OTP_REQUEST_REGISTRATION))
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
        otpAttempts.add(new OtpAttempt(identifierType, identifierValue, OtpAttempt.AttemptStatus.SUCCESS, currentTimestamp.minusMinutes(1), OtpAttempt.Action.OTP_REQUEST_REGISTRATION, cmId));
        otpAttempts.add(new OtpAttempt(identifierType, identifierValue, OtpAttempt.AttemptStatus.SUCCESS, currentTimestamp.minusMinutes(2), OtpAttempt.Action.OTP_REQUEST_REGISTRATION, cmId));
        otpAttempts.add(new OtpAttempt(identifierType, identifierValue, OtpAttempt.AttemptStatus.SUCCESS, currentTimestamp.minusMinutes(3), OtpAttempt.Action.OTP_REQUEST_REGISTRATION, cmId));
        otpAttempts.add(new OtpAttempt(identifierType, identifierValue, OtpAttempt.AttemptStatus.SUCCESS, currentTimestamp.minusMinutes(4), OtpAttempt.Action.OTP_REQUEST_REGISTRATION, cmId));
        otpAttempts.add(new OtpAttempt(identifierType, identifierValue, OtpAttempt.AttemptStatus.SUCCESS, currentTimestamp.minusMinutes(5), OtpAttempt.Action.OTP_REQUEST_REGISTRATION, cmId));
        when(otpAttemptRepository.getOtpAttempts(cmId, identifierType, identifierValue, userServiceProperties.getMaxOtpAttempts(), OtpAttempt.Action.OTP_REQUEST_REGISTRATION))
                .thenReturn(Mono.just(otpAttempts));
        when(otpAttemptRepository.insert(eq(cmId), eq(identifierType), eq(identifierValue), eq(OtpAttempt.AttemptStatus.FAILURE), eq(OtpAttempt.Action.OTP_REQUEST_REGISTRATION))).thenReturn(Mono.empty());
        StepVerifier.create(otpAttemptService.validateOTPRequest(identifierType, identifierValue, OtpAttempt.Action.OTP_REQUEST_REGISTRATION))
                .expectErrorMatches(throwable -> throwable instanceof ClientError && ((ClientError) throwable)
                        .getError()
                        .getError()
                        .getCode()
                        .equals(ErrorCode.OTP_REQUEST_LIMIT_EXCEEDED))
                .verify();
    }
}
