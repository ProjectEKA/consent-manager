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
        return otpAttemptRepository.getOtpAttempts(cmId, identifierType, identifierValue, userServiceProperties.getMaxOtpAttempts(), action)
                .filter(otpAttempts -> otpAttempts.size() == userServiceProperties.getMaxOtpAttempts())
                .filter(this::isNoneBlockedExceptLatest)
                .flatMap(this::validateLatestAttempt)
                .flatMap(this::validateAttemptsLimit)
                .switchIfEmpty(Mono.defer(() -> createOtpAttemptFor("", cmId, identifierType, identifierValue, OtpAttempt.AttemptStatus.SUCCESS, action)));
    }

    public Mono<Void> validateOTPRequest(String identifierType, String identifierValue, OtpAttempt.Action action) {
        return validateOTPRequest(identifierType, identifierValue, action, "");
    }

    public Mono<Void> validateOTPSubmission(OtpAttempt otpAttempt){
        return otpAttemptRepository.getOtpAttempts(otpAttempt.getCmId(),
                otpAttempt.getIdentifierType(),
                otpAttempt.getIdentifierValue(),
                userServiceProperties.getOtpMaxInvalidAttempts(),
                otpAttempt.getAction())
                .filter(otpRequestAttempts -> otpRequestAttempts.size() == userServiceProperties.getOtpMaxInvalidAttempts())
                .flatMap(otpRequestAttempts -> {
                    OtpAttempt latestAttempt = otpRequestAttempts.stream().findFirst().get();
                    boolean isBlocked = isWithinTimeLimit(latestAttempt, userServiceProperties.getOtpInvalidAttemptsBlockPeriodInMin());
                    return isBlocked ? Mono.error(ClientError.tooManyInvalidOtpAttempts()) : removeMatchingAttempts(otpAttempt).then(Mono.empty());
                });
    }

    public Mono<Void> removeMatchingAttempts(OtpAttempt otpAttempt){
        return otpAttemptRepository.removeAttempts(otpAttempt);
    }

    public Mono<Void> createOtpAttemptFor(String sessionId, String cmId, String identifierType, String identifierValue, OtpAttempt.AttemptStatus attemptStatus, OtpAttempt.Action action) {
        return otpAttemptRepository.insert(OtpAttempt.builder()
                .cmId(cmId)
                .identifierType(identifierType)
                .identifierValue(identifierValue)
                .attemptStatus(attemptStatus)
                .action(action)
                .build());
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
            return createOtpAttemptFor("", firstAttempt.getCmId(), firstAttempt.getIdentifierType(), firstAttempt.getIdentifierValue(), OtpAttempt.AttemptStatus.FAILURE, firstAttempt.getAction())
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
