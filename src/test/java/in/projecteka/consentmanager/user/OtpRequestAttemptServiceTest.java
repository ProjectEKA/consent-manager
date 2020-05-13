package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.model.ErrorCode;
import in.projecteka.consentmanager.user.model.OtpRequestAttempt;
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

class OtpRequestAttemptServiceTest {

    @Mock
    OtpRequestAttemptRepository otpRequestAttemptRepository;


    private UserServiceProperties userServiceProperties;

    private OtpRequestAttemptService otpRequestAttemptService;

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
        otpRequestAttemptService = new OtpRequestAttemptService(otpRequestAttemptRepository, userServiceProperties);
        identifierValue = "+91-6666666666";
        cmId = "";
        identifierType = "mobile";
    }

    @Test
    public void shouldInsertOTPAttemptForFirstNAttempts() { // where N is maxOtpAttempts
        when(otpRequestAttemptRepository.getOtpAttempts(cmId, identifierType, identifierValue, userServiceProperties.getMaxOtpAttempts(), OtpRequestAttempt.Action.REGISTRATION))
                .thenReturn(Mono.just(new ArrayList<>()));
        when(otpRequestAttemptRepository.insert(eq(cmId), eq(identifierType), eq(identifierValue), eq(OtpRequestAttempt.AttemptStatus.SUCCESS), eq(OtpRequestAttempt.Action.REGISTRATION))).thenReturn(Mono.empty());

        StepVerifier.create(otpRequestAttemptService.validateOTPRequest(identifierType, identifierValue, OtpRequestAttempt.Action.REGISTRATION))
                .verifyComplete();
    }

    @Test
    void shouldInsertOTPAttemptIfAnyOfTheAttemptIsBlockedExcludingLatest() {
        var otpAttempts = new ArrayList<OtpRequestAttempt>();
        otpAttempts.add(new OtpRequestAttempt(identifierType, identifierValue, OtpRequestAttempt.AttemptStatus.SUCCESS, LocalDateTime.now(), OtpRequestAttempt.Action.REGISTRATION, cmId));
        otpAttempts.add(new OtpRequestAttempt(identifierType, identifierValue, OtpRequestAttempt.AttemptStatus.SUCCESS, LocalDateTime.now(), OtpRequestAttempt.Action.REGISTRATION, cmId));
        otpAttempts.add(new OtpRequestAttempt(identifierType, identifierValue, OtpRequestAttempt.AttemptStatus.FAILURE, LocalDateTime.now(), OtpRequestAttempt.Action.REGISTRATION, cmId));
        otpAttempts.add(new OtpRequestAttempt(identifierType, identifierValue, OtpRequestAttempt.AttemptStatus.SUCCESS, LocalDateTime.now(), OtpRequestAttempt.Action.REGISTRATION, cmId));
        otpAttempts.add(new OtpRequestAttempt(identifierType, identifierValue, OtpRequestAttempt.AttemptStatus.SUCCESS, LocalDateTime.now(), OtpRequestAttempt.Action.REGISTRATION, cmId));
        when(otpRequestAttemptRepository.getOtpAttempts(cmId, identifierType, identifierValue, userServiceProperties.getMaxOtpAttempts(), OtpRequestAttempt.Action.REGISTRATION))
                .thenReturn(Mono.just(otpAttempts));
        when(otpRequestAttemptRepository.insert(eq(cmId), eq(identifierType), eq(identifierValue), eq(OtpRequestAttempt.AttemptStatus.SUCCESS), eq(OtpRequestAttempt.Action.REGISTRATION))).thenReturn(Mono.empty());
        StepVerifier.create(otpRequestAttemptService.validateOTPRequest(identifierType, identifierValue, OtpRequestAttempt.Action.REGISTRATION))
                .verifyComplete();
    }

    @Test
    void shouldInsertOTPAttemptWhenLatestAttemptIsBlockedAndBlockingTimeIsPassed() {
        var otpAttempts = new ArrayList<OtpRequestAttempt>();
        var currentTimestamp = LocalDateTime.now(ZoneOffset.UTC);
        otpAttempts.add(new OtpRequestAttempt(identifierType, identifierValue, OtpRequestAttempt.AttemptStatus.FAILURE, currentTimestamp.minusMinutes(5), OtpRequestAttempt.Action.REGISTRATION, cmId));
        otpAttempts.add(new OtpRequestAttempt(identifierType, identifierValue, OtpRequestAttempt.AttemptStatus.SUCCESS, currentTimestamp.minusMinutes(6), OtpRequestAttempt.Action.REGISTRATION, cmId));
        otpAttempts.add(new OtpRequestAttempt(identifierType, identifierValue, OtpRequestAttempt.AttemptStatus.SUCCESS, currentTimestamp.minusMinutes(7), OtpRequestAttempt.Action.REGISTRATION, cmId));
        otpAttempts.add(new OtpRequestAttempt(identifierType, identifierValue, OtpRequestAttempt.AttemptStatus.SUCCESS, currentTimestamp.minusMinutes(8), OtpRequestAttempt.Action.REGISTRATION, cmId));
        otpAttempts.add(new OtpRequestAttempt(identifierType, identifierValue, OtpRequestAttempt.AttemptStatus.SUCCESS, currentTimestamp.minusMinutes(9), OtpRequestAttempt.Action.REGISTRATION, cmId));
        when(otpRequestAttemptRepository.getOtpAttempts(cmId, identifierType, identifierValue, userServiceProperties.getMaxOtpAttempts(), OtpRequestAttempt.Action.REGISTRATION))
                .thenReturn(Mono.just(otpAttempts));
        when(otpRequestAttemptRepository.insert(eq(cmId), eq(identifierType), eq(identifierValue), eq(OtpRequestAttempt.AttemptStatus.SUCCESS), eq(OtpRequestAttempt.Action.REGISTRATION))).thenReturn(Mono.empty());
        StepVerifier.create(otpRequestAttemptService.validateOTPRequest(identifierType, identifierValue, OtpRequestAttempt.Action.REGISTRATION))
                .verifyComplete();
    }

    @Test
    void shouldInsertOTPAttemptWhenMaxOTPAttemptsLimitIsNotReached() {
        var otpAttempts = new ArrayList<OtpRequestAttempt>();
        var currentTimestamp = LocalDateTime.now(ZoneOffset.UTC);
        otpAttempts.add(new OtpRequestAttempt(identifierType, identifierValue, OtpRequestAttempt.AttemptStatus.SUCCESS, currentTimestamp.minusMinutes(5), OtpRequestAttempt.Action.REGISTRATION, cmId));
        otpAttempts.add(new OtpRequestAttempt(identifierType, identifierValue, OtpRequestAttempt.AttemptStatus.SUCCESS, currentTimestamp.minusMinutes(6), OtpRequestAttempt.Action.REGISTRATION, cmId));
        otpAttempts.add(new OtpRequestAttempt(identifierType, identifierValue, OtpRequestAttempt.AttemptStatus.SUCCESS, currentTimestamp.minusMinutes(9), OtpRequestAttempt.Action.REGISTRATION, cmId));
        otpAttempts.add(new OtpRequestAttempt(identifierType, identifierValue, OtpRequestAttempt.AttemptStatus.SUCCESS, currentTimestamp.minusMinutes(12), OtpRequestAttempt.Action.REGISTRATION, cmId));
        otpAttempts.add(new OtpRequestAttempt(identifierType, identifierValue, OtpRequestAttempt.AttemptStatus.SUCCESS, currentTimestamp.minusMinutes(17), OtpRequestAttempt.Action.REGISTRATION, cmId));
        when(otpRequestAttemptRepository.getOtpAttempts(cmId, identifierType, identifierValue, userServiceProperties.getMaxOtpAttempts(), OtpRequestAttempt.Action.REGISTRATION))
                .thenReturn(Mono.just(otpAttempts));
        when(otpRequestAttemptRepository.insert(eq(cmId), eq(identifierType), eq(identifierValue), eq(OtpRequestAttempt.AttemptStatus.SUCCESS), eq(OtpRequestAttempt.Action.REGISTRATION))).thenReturn(Mono.empty());
        StepVerifier.create(otpRequestAttemptService.validateOTPRequest(identifierType, identifierValue, OtpRequestAttempt.Action.REGISTRATION))
                .verifyComplete();
    }

    @Test
    void shouldThrowLimitExceededErrorWhenBlockingTimeIsNotPassed() {
        var otpAttempts = new ArrayList<OtpRequestAttempt>();
        var currentTimestamp = LocalDateTime.now(ZoneOffset.UTC);
        otpAttempts.add(new OtpRequestAttempt(identifierType, identifierValue, OtpRequestAttempt.AttemptStatus.FAILURE, currentTimestamp.minusMinutes(1), OtpRequestAttempt.Action.REGISTRATION, cmId));
        otpAttempts.add(new OtpRequestAttempt(identifierType, identifierValue, OtpRequestAttempt.AttemptStatus.SUCCESS, currentTimestamp.minusMinutes(2), OtpRequestAttempt.Action.REGISTRATION, cmId));
        otpAttempts.add(new OtpRequestAttempt(identifierType, identifierValue, OtpRequestAttempt.AttemptStatus.SUCCESS, currentTimestamp.minusMinutes(3), OtpRequestAttempt.Action.REGISTRATION, cmId));
        otpAttempts.add(new OtpRequestAttempt(identifierType, identifierValue, OtpRequestAttempt.AttemptStatus.SUCCESS, currentTimestamp.minusMinutes(4), OtpRequestAttempt.Action.REGISTRATION, cmId));
        otpAttempts.add(new OtpRequestAttempt(identifierType, identifierValue, OtpRequestAttempt.AttemptStatus.SUCCESS, currentTimestamp.minusMinutes(5), OtpRequestAttempt.Action.REGISTRATION, cmId));
        when(otpRequestAttemptRepository.getOtpAttempts(cmId, identifierType, identifierValue, userServiceProperties.getMaxOtpAttempts(), OtpRequestAttempt.Action.REGISTRATION))
                .thenReturn(Mono.just(otpAttempts));
        StepVerifier.create(otpRequestAttemptService.validateOTPRequest(identifierType, identifierValue, OtpRequestAttempt.Action.REGISTRATION))
                .expectErrorMatches(throwable -> throwable instanceof ClientError && ((ClientError) throwable)
                        .getError()
                        .getError()
                        .getCode()
                        .equals(ErrorCode.OTP_REQUEST_LIMIT_EXCEEDED))
                .verify();
    }

    @Test
    void shouldThrowLimitExceededErrorOnExceedingMaxOTPAttempts() {
        var otpAttempts = new ArrayList<OtpRequestAttempt>();
        var currentTimestamp = LocalDateTime.now(ZoneOffset.UTC);
        otpAttempts.add(new OtpRequestAttempt(identifierType, identifierValue, OtpRequestAttempt.AttemptStatus.SUCCESS, currentTimestamp.minusMinutes(1), OtpRequestAttempt.Action.REGISTRATION, cmId));
        otpAttempts.add(new OtpRequestAttempt(identifierType, identifierValue, OtpRequestAttempt.AttemptStatus.SUCCESS, currentTimestamp.minusMinutes(2), OtpRequestAttempt.Action.REGISTRATION, cmId));
        otpAttempts.add(new OtpRequestAttempt(identifierType, identifierValue, OtpRequestAttempt.AttemptStatus.SUCCESS, currentTimestamp.minusMinutes(3), OtpRequestAttempt.Action.REGISTRATION, cmId));
        otpAttempts.add(new OtpRequestAttempt(identifierType, identifierValue, OtpRequestAttempt.AttemptStatus.SUCCESS, currentTimestamp.minusMinutes(4), OtpRequestAttempt.Action.REGISTRATION, cmId));
        otpAttempts.add(new OtpRequestAttempt(identifierType, identifierValue, OtpRequestAttempt.AttemptStatus.SUCCESS, currentTimestamp.minusMinutes(5), OtpRequestAttempt.Action.REGISTRATION, cmId));
        when(otpRequestAttemptRepository.getOtpAttempts(cmId, identifierType, identifierValue, userServiceProperties.getMaxOtpAttempts(), OtpRequestAttempt.Action.REGISTRATION))
                .thenReturn(Mono.just(otpAttempts));
        when(otpRequestAttemptRepository.insert(eq(cmId), eq(identifierType), eq(identifierValue), eq(OtpRequestAttempt.AttemptStatus.FAILURE), eq(OtpRequestAttempt.Action.REGISTRATION))).thenReturn(Mono.empty());
        StepVerifier.create(otpRequestAttemptService.validateOTPRequest(identifierType, identifierValue, OtpRequestAttempt.Action.REGISTRATION))
                .expectErrorMatches(throwable -> throwable instanceof ClientError && ((ClientError) throwable)
                        .getError()
                        .getError()
                        .getCode()
                        .equals(ErrorCode.OTP_REQUEST_LIMIT_EXCEEDED))
                .verify();
    }
}