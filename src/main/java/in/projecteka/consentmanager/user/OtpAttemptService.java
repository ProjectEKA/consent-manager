package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.user.model.OtpAttempt;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class OtpAttemptService {

    private final OtpAttemptRepository otpAttemptRepository;
    private final UserServiceProperties userServiceProperties;

    public Mono<Void> validateOTPRequest(String identifierType, String identifierValue, OtpAttempt.Action action, String cmId) {
        OtpAttempt.OtpAttemptBuilder builder = OtpAttempt.builder()
                .identifierType(identifierType)
                .identifierValue(identifierValue)
                .action(action)
                .cmId(cmId);
        return otpAttemptRepository.getOtpAttempts(builder.build(), userServiceProperties.getMaxOtpAttempts())
                .filter(otpAttempts -> otpAttempts.size() == userServiceProperties.getMaxOtpAttempts())
                .filter(this::isNoneBlockedExceptLatest)
                .flatMap(this::validateLatestAttempt)
                .flatMap(this::validateAttemptsLimit)
                .switchIfEmpty(Mono.defer(() -> saveOTPAttempt(builder.attemptStatus(OtpAttempt.AttemptStatus.SUCCESS).build())));
    }

    public Mono<Void> validateOTPRequest(String identifierType, String identifierValue, OtpAttempt.Action action) {
        return validateOTPRequest(identifierType, identifierValue, action, "");
    }

    public Mono<Void> validateOTPSubmission(OtpAttempt otpAttempt) {
        return otpAttemptRepository.getOtpAttempts(otpAttempt, userServiceProperties.getOtpMaxInvalidAttempts())
                .filter(otpRequestAttempts -> otpRequestAttempts.size() == userServiceProperties.getOtpMaxInvalidAttempts())
                .flatMap(otpRequestAttempts -> {
                    OtpAttempt latestAttempt = otpRequestAttempts.stream().findFirst().get();
                    boolean isBlocked = isWithinTimeLimit(latestAttempt, userServiceProperties.getOtpInvalidAttemptsBlockPeriodInMin());
                    return isBlocked ? Mono.error(ClientError.tooManyInvalidOtpAttempts()) : removeMatchingAttempts(otpAttempt);
                });
    }

    public Mono<Void> removeMatchingAttempts(OtpAttempt otpAttempt) {
        return otpAttemptRepository.removeAttempts(otpAttempt);
    }

    public Mono<Void> saveOTPAttempt(OtpAttempt attempt) {
        return otpAttemptRepository.insert(attempt);
    }

    private Mono<List<OtpAttempt>> validateLatestAttempt(List<OtpAttempt> attempts) {
        OtpAttempt latestAttempt = attempts.get(0);
        if (latestAttempt.getAttemptStatus() == OtpAttempt.AttemptStatus.FAILURE) {
            return handleBlockedLatestAttempt(latestAttempt);
        }
        return Mono.just(attempts);
    }

    private Mono<List<OtpAttempt>> handleBlockedLatestAttempt(OtpAttempt latestAttempt) {
        boolean isInBlockingTime = isWithinTimeLimit(latestAttempt, userServiceProperties.getOtpAttemptsBlockPeriodInMin());
        if (isInBlockingTime) {
            return Mono.error(ClientError.otpRequestLimitExceeded());
        }
        return Mono.empty();
    }

    private Mono<Void> validateAttemptsLimit(List<OtpAttempt> attempts) {
        OtpAttempt firstAttempt = attempts.get(attempts.size() - 1);
        boolean isAttemptsLimitExceeded = isWithinTimeLimit(firstAttempt, userServiceProperties.getMaxOtpAttemptsPeriodInMin());
        if (isAttemptsLimitExceeded) {
            return saveOTPAttempt(firstAttempt.toBuilder().attemptStatus(OtpAttempt.AttemptStatus.FAILURE).build())
                    .then(Mono.error(ClientError.otpRequestLimitExceeded()));
        }
        return Mono.empty();
    }

    private boolean isNoneBlockedExceptLatest(List<OtpAttempt> otpAttempts) {
        List<OtpAttempt> duplicateAttempts = new ArrayList<>(otpAttempts);
        duplicateAttempts.remove(0); //remove latest otp attempt
        return duplicateAttempts.stream().noneMatch(attempt -> attempt.getAttemptStatus() == OtpAttempt.AttemptStatus.FAILURE);
    }

    public boolean isWithinTimeLimit(OtpAttempt attempt, int timeLimitInMin) {
        return attempt
                .getAttemptAt()
                .plusMinutes(timeLimitInMin)
                .isAfter(LocalDateTime.now(ZoneOffset.UTC));
    }
}
