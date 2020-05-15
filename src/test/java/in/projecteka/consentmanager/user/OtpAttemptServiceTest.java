package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.model.ErrorCode;
import in.projecteka.consentmanager.user.model.OtpAttempt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
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
                5,
                2,
                5);
        otpAttemptService = new OtpAttemptService(otpAttemptRepository, userServiceProperties);
        identifierValue = "+91-6666666666";
        cmId = "";
        identifierType = "mobile";
    }

    @Test
    public void shouldInsertOTPAttemptForFirstNAttempts() { // where N is maxOtpAttempts
        ArgumentCaptor<OtpAttempt> argument = ArgumentCaptor.forClass(OtpAttempt.class);
        when(otpAttemptRepository.getOtpAttempts(argument.capture(), eq(userServiceProperties.getMaxOtpAttempts())))
                .thenReturn(Mono.just(new ArrayList<>()));
        when(otpAttemptRepository.insert(argument.capture())).thenReturn(Mono.empty());
        StepVerifier.create(otpAttemptService.validateOTPRequest(identifierType, identifierValue, OtpAttempt.Action.OTP_REQUEST_REGISTRATION))
                .verifyComplete();
        var capturedAttempts = argument.getAllValues();
        var getAttemptsArgument = capturedAttempts.get(0);
        var insertArgument = capturedAttempts.get(1);
        assertEquals(cmId, getAttemptsArgument.getCmId());
        assertEquals(identifierType.toUpperCase(), getAttemptsArgument.getIdentifierType());
        assertEquals(identifierValue, getAttemptsArgument.getIdentifierValue());
        assertEquals(OtpAttempt.Action.OTP_REQUEST_REGISTRATION, getAttemptsArgument.getAction());
        assertEquals(cmId, insertArgument.getCmId());
        assertEquals(identifierType.toUpperCase(), insertArgument.getIdentifierType());
        assertEquals(identifierValue, insertArgument.getIdentifierValue());
        assertEquals(OtpAttempt.Action.OTP_REQUEST_REGISTRATION, insertArgument.getAction());
    }

    @Test
    void shouldInsertOTPAttemptIfAnyOfTheAttemptIsBlockedExcludingLatest() {
        ArgumentCaptor<OtpAttempt> argument = ArgumentCaptor.forClass(OtpAttempt.class);
        var otpAttempts = new ArrayList<OtpAttempt>();
        var builder = OtpAttempt.builder()
                .cmId(cmId)
                .identifierType(identifierType)
                .identifierValue(identifierValue)
                .attemptAt(LocalDateTime.now())
                .action(OtpAttempt.Action.OTP_REQUEST_REGISTRATION);
        OtpAttempt successfulAttempt = builder.attemptStatus(OtpAttempt.AttemptStatus.SUCCESS).build();
        OtpAttempt failedAttempt = builder.attemptStatus(OtpAttempt.AttemptStatus.FAILURE).build();
        otpAttempts.add(successfulAttempt);
        otpAttempts.add(successfulAttempt);
        otpAttempts.add(failedAttempt);
        otpAttempts.add(successfulAttempt);
        otpAttempts.add(successfulAttempt);
        when(otpAttemptRepository.getOtpAttempts(argument.capture(), eq(userServiceProperties.getMaxOtpAttempts())))
                .thenReturn(Mono.just(otpAttempts));
        when(otpAttemptRepository.insert(argument.capture())).thenReturn(Mono.empty());
        StepVerifier.create(otpAttemptService.validateOTPRequest(identifierType, identifierValue, OtpAttempt.Action.OTP_REQUEST_REGISTRATION))
                .verifyComplete();

        var capturedAttempts = argument.getAllValues();
        var getAttemptsArgument = capturedAttempts.get(0);
        var insertArgument = capturedAttempts.get(1);
        assertEquals(cmId, getAttemptsArgument.getCmId());
        assertEquals(identifierType.toUpperCase(), getAttemptsArgument.getIdentifierType());
        assertEquals(identifierValue, getAttemptsArgument.getIdentifierValue());
        assertEquals(OtpAttempt.Action.OTP_REQUEST_REGISTRATION, getAttemptsArgument.getAction());
        assertEquals(cmId, insertArgument.getCmId());
        assertEquals(identifierType.toUpperCase(), insertArgument.getIdentifierType());
        assertEquals(identifierValue, insertArgument.getIdentifierValue());
        assertEquals(OtpAttempt.Action.OTP_REQUEST_REGISTRATION, insertArgument.getAction());
    }

    @Test
    void shouldInsertOTPAttemptWhenLatestAttemptIsBlockedAndBlockingTimeIsPassed() {
        ArgumentCaptor<OtpAttempt> argument = ArgumentCaptor.forClass(OtpAttempt.class);
        var currentTimestamp = LocalDateTime.now(ZoneOffset.UTC);
        var builder = OtpAttempt.builder()
                .cmId(cmId)
                .identifierType(identifierType)
                .identifierValue(identifierValue)
                .attemptAt(currentTimestamp.minusMinutes(3))
                .action(OtpAttempt.Action.OTP_REQUEST_REGISTRATION);
        OtpAttempt successfulAttempt = builder.attemptStatus(OtpAttempt.AttemptStatus.SUCCESS).build();
        OtpAttempt failedAttempt = builder.attemptStatus(OtpAttempt.AttemptStatus.FAILURE).build();
        var otpAttempts = Arrays.asList(failedAttempt, successfulAttempt, successfulAttempt, successfulAttempt, successfulAttempt);
        when(otpAttemptRepository.getOtpAttempts(argument.capture(), eq(userServiceProperties.getMaxOtpAttempts())))
                .thenReturn(Mono.just(otpAttempts));
        when(otpAttemptRepository.insert(argument.capture())).thenReturn(Mono.empty());
        StepVerifier.create(otpAttemptService.validateOTPRequest(identifierType, identifierValue, OtpAttempt.Action.OTP_REQUEST_REGISTRATION))
                .verifyComplete();

        var capturedAttempts = argument.getAllValues();
        var getAttemptsArgument = capturedAttempts.get(0);
        var insertArgument = capturedAttempts.get(1);
        assertEquals(cmId, getAttemptsArgument.getCmId());
        assertEquals(identifierType.toUpperCase(), getAttemptsArgument.getIdentifierType());
        assertEquals(identifierValue, getAttemptsArgument.getIdentifierValue());
        assertEquals(OtpAttempt.Action.OTP_REQUEST_REGISTRATION, getAttemptsArgument.getAction());
        assertEquals(cmId, insertArgument.getCmId());
        assertEquals(identifierType.toUpperCase(), insertArgument.getIdentifierType());
        assertEquals(identifierValue, insertArgument.getIdentifierValue());
        assertEquals(OtpAttempt.Action.OTP_REQUEST_REGISTRATION, insertArgument.getAction());
    }

    @Test
    void shouldInsertOTPAttemptWhenMaxOTPAttemptsLimitIsNotReached() {
        ArgumentCaptor<OtpAttempt> argument = ArgumentCaptor.forClass(OtpAttempt.class);
        var currentTimestamp = LocalDateTime.now(ZoneOffset.UTC);
        var builder = OtpAttempt.builder()
                .cmId(cmId)
                .identifierType(identifierType)
                .identifierValue(identifierValue)
                .action(OtpAttempt.Action.OTP_REQUEST_REGISTRATION)
                .attemptStatus(OtpAttempt.AttemptStatus.SUCCESS);
        var otpAttempts = Arrays.asList(builder.attemptAt(currentTimestamp.minusMinutes(1)).build(),
                builder.attemptAt(currentTimestamp.minusMinutes(4)).build(),
                builder.attemptAt(currentTimestamp.minusMinutes(5)).build(),
                builder.attemptAt(currentTimestamp.minusMinutes(9)).build(),
                builder.attemptAt(currentTimestamp.minusMinutes(11)).build());
        when(otpAttemptRepository.getOtpAttempts(argument.capture(), eq(userServiceProperties.getMaxOtpAttempts())))
                .thenReturn(Mono.just(otpAttempts));
        when(otpAttemptRepository.insert(argument.capture())).thenReturn(Mono.empty());
        StepVerifier.create(otpAttemptService.validateOTPRequest(identifierType, identifierValue, OtpAttempt.Action.OTP_REQUEST_REGISTRATION))
                .verifyComplete();

        var capturedAttempts = argument.getAllValues();
        var getAttemptsArgument = capturedAttempts.get(0);
        var insertArgument = capturedAttempts.get(1);
        assertEquals(cmId, getAttemptsArgument.getCmId());
        assertEquals(identifierType.toUpperCase(), getAttemptsArgument.getIdentifierType());
        assertEquals(identifierValue, getAttemptsArgument.getIdentifierValue());
        assertEquals(OtpAttempt.Action.OTP_REQUEST_REGISTRATION, getAttemptsArgument.getAction());
        assertEquals(cmId, insertArgument.getCmId());
        assertEquals(identifierType.toUpperCase(), insertArgument.getIdentifierType());
        assertEquals(identifierValue, insertArgument.getIdentifierValue());
        assertEquals(OtpAttempt.Action.OTP_REQUEST_REGISTRATION, insertArgument.getAction());
    }

    @Test
    void shouldThrowLimitExceededErrorWhenBlockingTimeIsNotPassed() {
        ArgumentCaptor<OtpAttempt> argument = ArgumentCaptor.forClass(OtpAttempt.class);
        var currentTimestamp = LocalDateTime.now(ZoneOffset.UTC);
        var builder = OtpAttempt.builder()
                .cmId(cmId)
                .identifierType(identifierType)
                .identifierValue(identifierValue)
                .attemptAt(currentTimestamp.minusMinutes(1))
                .action(OtpAttempt.Action.OTP_REQUEST_REGISTRATION);
        OtpAttempt successfulAttempt = builder.attemptStatus(OtpAttempt.AttemptStatus.SUCCESS).build();
        OtpAttempt failedAttempt = builder.attemptStatus(OtpAttempt.AttemptStatus.FAILURE).build();
        var otpAttempts = Arrays.asList(failedAttempt, successfulAttempt, successfulAttempt, successfulAttempt, successfulAttempt);

        when(otpAttemptRepository.getOtpAttempts(argument.capture(), eq(userServiceProperties.getMaxOtpAttempts())))
                .thenReturn(Mono.just(otpAttempts));
        StepVerifier.create(otpAttemptService.validateOTPRequest(identifierType, identifierValue, OtpAttempt.Action.OTP_REQUEST_REGISTRATION))
                .expectErrorMatches(throwable -> throwable instanceof ClientError && ((ClientError) throwable)
                        .getError()
                        .getError()
                        .getCode()
                        .equals(ErrorCode.OTP_REQUEST_LIMIT_EXCEEDED))
                .verify();

        assertEquals(cmId, argument.getValue().getCmId());
        assertEquals(identifierType.toUpperCase(), argument.getValue().getIdentifierType());
        assertEquals(identifierValue, argument.getValue().getIdentifierValue());
        assertEquals(OtpAttempt.Action.OTP_REQUEST_REGISTRATION, argument.getValue().getAction());
    }

    @Test
    void shouldThrowLimitExceededErrorOnExceedingMaxOTPAttempts() {
        ArgumentCaptor<OtpAttempt> argument = ArgumentCaptor.forClass(OtpAttempt.class);
        var currentTimestamp = LocalDateTime.now(ZoneOffset.UTC);
        var builder = OtpAttempt.builder()
                .cmId(cmId)
                .identifierType(identifierType)
                .identifierValue(identifierValue)
                .attemptAt(currentTimestamp.minusMinutes(3))
                .action(OtpAttempt.Action.OTP_REQUEST_REGISTRATION);
        OtpAttempt successfulAttempt = builder.attemptStatus(OtpAttempt.AttemptStatus.SUCCESS).build();
        var otpAttempts = Arrays.asList(successfulAttempt, successfulAttempt, successfulAttempt, successfulAttempt, successfulAttempt);

        when(otpAttemptRepository.getOtpAttempts(argument.capture(), eq(userServiceProperties.getMaxOtpAttempts())))
                .thenReturn(Mono.just(otpAttempts));
        when(otpAttemptRepository.insert(argument.capture())).thenReturn(Mono.empty());
        StepVerifier.create(otpAttemptService.validateOTPRequest(identifierType, identifierValue, OtpAttempt.Action.OTP_REQUEST_REGISTRATION))
                .expectErrorMatches(throwable -> throwable instanceof ClientError && ((ClientError) throwable)
                        .getError()
                        .getError()
                        .getCode()
                        .equals(ErrorCode.OTP_REQUEST_LIMIT_EXCEEDED))
                .verify();

        assertEquals(cmId, argument.getValue().getCmId());
        assertEquals(identifierType.toUpperCase(), argument.getValue().getIdentifierType());
        assertEquals(identifierValue, argument.getValue().getIdentifierValue());
        assertEquals(OtpAttempt.Action.OTP_REQUEST_REGISTRATION, argument.getValue().getAction());
    }
}
