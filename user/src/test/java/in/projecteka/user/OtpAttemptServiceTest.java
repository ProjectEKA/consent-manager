package in.projecteka.user;

import in.projecteka.library.clients.model.ClientError;
import in.projecteka.library.clients.model.ErrorCode;
import in.projecteka.user.model.OtpAttempt;
import in.projecteka.user.properties.UserServiceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;

import static in.projecteka.user.TestBuilders.otpAttempt;
import static in.projecteka.user.model.OtpAttempt.Action.OTP_REQUEST_REGISTRATION;
import static in.projecteka.user.model.OtpAttempt.Action.OTP_SUBMIT_REGISTRATION;
import static in.projecteka.user.model.OtpAttempt.AttemptStatus.FAILURE;
import static in.projecteka.user.model.OtpAttempt.AttemptStatus.SUCCESS;
import static java.time.LocalDateTime.now;
import static java.time.ZoneOffset.UTC;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.just;
import static reactor.test.StepVerifier.create;

class OtpAttemptServiceTest {

    @Mock
    OtpAttemptRepository otpAttemptRepository;

    private UserServiceProperties userServiceProperties;

    private OtpAttemptService otpAttemptService;

    private String identifierType;

    private String identifierValue;

    private ArgumentCaptor<OtpAttempt> argument;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        userServiceProperties = new UserServiceProperties(
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
        identifierType = "mobile";
        argument = ArgumentCaptor.forClass(OtpAttempt.class);
    }

    @Test
    void shouldInsertOTPAttemptForFirstNAttempts() { // where N is maxOtpAttempts
        when(otpAttemptRepository.getOtpAttempts(argument.capture(), eq(userServiceProperties.getMaxOtpAttempts())))
                .thenReturn(just(new ArrayList<>()));
        when(otpAttemptRepository.insert(argument.capture())).thenReturn(empty());

        create(otpAttemptService.validateOTPRequest(identifierType, identifierValue, OTP_REQUEST_REGISTRATION))
                .verifyComplete();

        var capturedAttempts = argument.getAllValues();
        var getAttemptsArgument = capturedAttempts.get(0);
        var insertArgument = capturedAttempts.get(1);
        assertEquals("", getAttemptsArgument.getCmId());
        assertEquals(identifierType.toUpperCase(), getAttemptsArgument.getIdentifierType());
        assertEquals(identifierValue, getAttemptsArgument.getIdentifierValue());
        assertEquals(OTP_REQUEST_REGISTRATION, getAttemptsArgument.getAction());
        assertEquals("", insertArgument.getCmId());
        assertEquals(identifierType.toUpperCase(), insertArgument.getIdentifierType());
        assertEquals(identifierValue, insertArgument.getIdentifierValue());
        assertEquals(OTP_REQUEST_REGISTRATION, insertArgument.getAction());
    }

    @Test
    void shouldInsertOTPAttemptIfAnyOfTheAttemptIsBlockedExcludingLatest() {
        var otpAttempts = new ArrayList<OtpAttempt>();
        var localBuilder = otpAttempt().attemptAt(now())
                .action(OTP_REQUEST_REGISTRATION);
        OtpAttempt successfulAttempt = localBuilder.attemptStatus(SUCCESS).build();
        OtpAttempt failedAttempt = localBuilder.attemptStatus(FAILURE).build();
        otpAttempts.add(successfulAttempt);
        otpAttempts.add(successfulAttempt);
        otpAttempts.add(failedAttempt);
        otpAttempts.add(successfulAttempt);
        otpAttempts.add(successfulAttempt);
        when(otpAttemptRepository.getOtpAttempts(argument.capture(), eq(userServiceProperties.getMaxOtpAttempts())))
                .thenReturn(just(otpAttempts));
        when(otpAttemptRepository.insert(argument.capture())).thenReturn(empty());

        create(otpAttemptService.validateOTPRequest(identifierType,
                identifierValue,
                OTP_REQUEST_REGISTRATION))
                .verifyComplete();

        var capturedAttempts = argument.getAllValues();
        var getAttemptsArgument = capturedAttempts.get(0);
        var insertArgument = capturedAttempts.get(1);
        assertEquals(successfulAttempt.getCmId(), getAttemptsArgument.getCmId());
        assertEquals(identifierType.toUpperCase(), getAttemptsArgument.getIdentifierType());
        assertEquals(identifierValue, getAttemptsArgument.getIdentifierValue());
        assertEquals(OTP_REQUEST_REGISTRATION, getAttemptsArgument.getAction());
        assertEquals(failedAttempt.getCmId(), insertArgument.getCmId());
        assertEquals(identifierType.toUpperCase(), insertArgument.getIdentifierType());
        assertEquals(identifierValue, insertArgument.getIdentifierValue());
        assertEquals(OTP_REQUEST_REGISTRATION, insertArgument.getAction());
    }

    @Test
    void shouldInsertOTPAttemptWhenLatestAttemptIsBlockedAndBlockingTimeIsPassed() {
        var localBuilder = otpAttempt()
                .attemptAt(now(UTC)
                        .minusMinutes(3))
                .action(OTP_REQUEST_REGISTRATION);
        OtpAttempt successfulAttempt = localBuilder.attemptStatus(SUCCESS).build();
        OtpAttempt failedAttempt = localBuilder.attemptStatus(FAILURE).build();
        var otpAttempts = asList(failedAttempt,
                successfulAttempt,
                successfulAttempt,
                successfulAttempt,
                successfulAttempt);
        when(otpAttemptRepository.getOtpAttempts(argument.capture(), eq(userServiceProperties.getMaxOtpAttempts())))
                .thenReturn(just(otpAttempts));
        when(otpAttemptRepository.insert(argument.capture())).thenReturn(empty());

        create(otpAttemptService.validateOTPRequest(identifierType, identifierValue, OTP_REQUEST_REGISTRATION))
                .verifyComplete();
        var capturedAttempts = argument.getAllValues();
        var getAttemptsArgument = capturedAttempts.get(0);
        var insertArgument = capturedAttempts.get(1);
        assertEquals("", getAttemptsArgument.getCmId());
        assertEquals(identifierType.toUpperCase(), getAttemptsArgument.getIdentifierType());
        assertEquals(identifierValue, getAttemptsArgument.getIdentifierValue());
        assertEquals(OTP_REQUEST_REGISTRATION, getAttemptsArgument.getAction());
        assertEquals("", insertArgument.getCmId());
        assertEquals(identifierType.toUpperCase(), insertArgument.getIdentifierType());
        assertEquals(identifierValue, insertArgument.getIdentifierValue());
        assertEquals(OTP_REQUEST_REGISTRATION, insertArgument.getAction());
    }

    @Test
    void shouldInsertOTPAttemptWhenMaxOTPAttemptsLimitIsNotReached() {
        var localBuilder = otpAttempt().action(OTP_REQUEST_REGISTRATION).attemptStatus(SUCCESS);
        var otpAttempts = asList(localBuilder.attemptAt(now(UTC).minusMinutes(1)).build(),
                localBuilder.attemptAt(now(UTC).minusMinutes(4)).build(),
                localBuilder.attemptAt(now(UTC).minusMinutes(5)).build(),
                localBuilder.attemptAt(now(UTC).minusMinutes(9)).build(),
                localBuilder.attemptAt(now(UTC).minusMinutes(11)).build());
        when(otpAttemptRepository.getOtpAttempts(argument.capture(), eq(userServiceProperties.getMaxOtpAttempts())))
                .thenReturn(just(otpAttempts));
        when(otpAttemptRepository.insert(argument.capture())).thenReturn(empty());

        create(otpAttemptService.validateOTPRequest(identifierType, identifierValue, OTP_REQUEST_REGISTRATION))
                .verifyComplete();

        var capturedAttempts = argument.getAllValues();
        var getAttemptsArgument = capturedAttempts.get(0);
        var insertArgument = capturedAttempts.get(1);
        assertEquals("", getAttemptsArgument.getCmId());
        assertEquals(identifierType.toUpperCase(), getAttemptsArgument.getIdentifierType());
        assertEquals(identifierValue, getAttemptsArgument.getIdentifierValue());
        assertEquals(OTP_REQUEST_REGISTRATION, getAttemptsArgument.getAction());
        assertEquals("", insertArgument.getCmId());
        assertEquals(identifierType.toUpperCase(), insertArgument.getIdentifierType());
        assertEquals(identifierValue, insertArgument.getIdentifierValue());
        assertEquals(OTP_REQUEST_REGISTRATION, insertArgument.getAction());
    }

    @Test
    void shouldThrowLimitExceededErrorWhenBlockingTimeIsNotPassed() {
        var localBuilder = otpAttempt()
                .attemptAt(now(UTC).minusMinutes(1))
                .action(OTP_REQUEST_REGISTRATION);
        var successfulAttempt = localBuilder.attemptStatus(SUCCESS).build();
        var failedAttempt = localBuilder.attemptStatus(FAILURE).build();
        var otpAttempts = asList(failedAttempt, successfulAttempt, successfulAttempt, successfulAttempt, successfulAttempt);
        when(otpAttemptRepository.getOtpAttempts(argument.capture(), eq(userServiceProperties.getMaxOtpAttempts())))
                .thenReturn(just(otpAttempts));

        create(otpAttemptService.validateOTPRequest(identifierType, identifierValue, OTP_REQUEST_REGISTRATION))
                .expectErrorMatches(throwable -> throwable instanceof ClientError && ((ClientError) throwable)
                        .getError()
                        .getError()
                        .getCode()
                        .equals(ErrorCode.OTP_REQUEST_LIMIT_EXCEEDED))
                .verify();

        assertEquals("", argument.getValue().getCmId());
        assertEquals(identifierType.toUpperCase(), argument.getValue().getIdentifierType());
        assertEquals(identifierValue, argument.getValue().getIdentifierValue());
        assertEquals(OTP_REQUEST_REGISTRATION, argument.getValue().getAction());
    }

    @Test
    void shouldThrowLimitExceededErrorOnExceedingMaxOTPAttempts() {
        var localBuilder = otpAttempt().attemptAt(now(UTC).minusMinutes(3)).action(OTP_REQUEST_REGISTRATION);
        var successfulAttempt = localBuilder.attemptStatus(SUCCESS).build();
        var otpAttempts = asList(successfulAttempt, successfulAttempt, successfulAttempt, successfulAttempt, successfulAttempt);

        when(otpAttemptRepository.getOtpAttempts(argument.capture(), eq(userServiceProperties.getMaxOtpAttempts())))
                .thenReturn(just(otpAttempts));
        when(otpAttemptRepository.insert(argument.capture())).thenReturn(empty());
        create(otpAttemptService.validateOTPRequest(identifierType, identifierValue, OTP_REQUEST_REGISTRATION))
                .expectErrorMatches(throwable -> throwable instanceof ClientError && ((ClientError) throwable)
                        .getError()
                        .getError()
                        .getCode()
                        .equals(ErrorCode.OTP_REQUEST_LIMIT_EXCEEDED))
                .verify();

        assertEquals(successfulAttempt.getCmId(), argument.getValue().getCmId());
        assertEquals(identifierType.toUpperCase(), argument.getValue().getIdentifierType());
        assertEquals(identifierValue, argument.getValue().getIdentifierValue());
        assertEquals(OTP_REQUEST_REGISTRATION, argument.getValue().getAction());
    }

    @Test
    void shouldRemoveMatchingAttempts() {
        var attempt = otpAttempt().action(OTP_REQUEST_REGISTRATION).build();
        when(otpAttemptRepository.removeAttempts(eq(attempt))).thenReturn(empty());

        create(otpAttemptService.removeMatchingAttempts(attempt))
                .verifyComplete();
        verify(otpAttemptRepository).removeAttempts(eq(attempt));
    }

    @Test
    void shouldPassTheValidationIfOtpSubmitAttemptsAreLessThanMaxOtpAttempts() {
        var localBuilder = otpAttempt().action(OTP_SUBMIT_REGISTRATION);

        var attempt = localBuilder.build();
        var failedAttempt = localBuilder.attemptStatus(FAILURE).build();
        var failedAttempts = asList(failedAttempt, failedAttempt, failedAttempt);
        when(otpAttemptRepository.getOtpAttempts(eq(attempt), eq(userServiceProperties.getOtpMaxInvalidAttempts()))).thenReturn(just(failedAttempts));

        create(otpAttemptService.validateOTPSubmission(attempt)).verifyComplete();
    }

    @Test
    void shouldPassTheValidationAndRemoveMatchingAttemptsIfLatestAttemptIsNotInBlockingPeriod() {
        var localBuilder = otpAttempt().action(OTP_SUBMIT_REGISTRATION);

        var attempt = localBuilder.build();
        var failedAttempt = localBuilder.attemptStatus(FAILURE).attemptAt(now(UTC).minusMinutes(3)).build();
        var failedAttempts = asList(failedAttempt, failedAttempt, failedAttempt, failedAttempt, failedAttempt);
        when(otpAttemptRepository.getOtpAttempts(eq(attempt), eq(userServiceProperties.getOtpMaxInvalidAttempts()))).thenReturn(just(failedAttempts));
        when(otpAttemptRepository.removeAttempts(eq(attempt))).thenReturn(empty());
        create(otpAttemptService.validateOTPSubmission(attempt)).verifyComplete();
    }

    @Test
    void shouldThrowErrorIfLatestAttemptIsInBlockingPeriod() {
        var localBuilder = otpAttempt().action(OTP_SUBMIT_REGISTRATION);
        var attempt = localBuilder.build();
        var failedAttempt = localBuilder.attemptStatus(FAILURE).attemptAt(now(UTC).minusMinutes(1)).build();
        var failedAttempts = asList(failedAttempt,
                failedAttempt,
                failedAttempt,
                failedAttempt,
                failedAttempt);
        when(otpAttemptRepository.getOtpAttempts(eq(attempt), eq(userServiceProperties.getOtpMaxInvalidAttempts())))
                .thenReturn(just(failedAttempts));

        create(otpAttemptService.validateOTPSubmission(attempt))
                .expectErrorMatches(throwable -> throwable instanceof ClientError && ((ClientError) throwable)
                        .getError()
                        .getError()
                        .getCode()
                        .equals(ErrorCode.INVALID_OTP_ATTEMPTS_EXCEEDED))
                .verify();
    }
}
