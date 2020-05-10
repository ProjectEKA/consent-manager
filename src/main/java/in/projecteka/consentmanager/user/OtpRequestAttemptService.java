package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.user.model.OtpRequestAttempt;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class OtpRequestAttemptService {

    private final OtpRequestAttemptRepository otpRequestAttemptRepository;
    private final UserServiceProperties userServiceProperties;

    public Mono<Void> validateOTPRequest(String identifierType, String identifierValue, OtpRequestAttempt.Action action, String cmId) {
        return otpRequestAttemptRepository.getOtpAttempts(cmId, identifierType, identifierValue, userServiceProperties.getMaxOtpAttempts(), action)
                .filter(otpAttempts -> otpAttempts.size() >= userServiceProperties.getMaxOtpAttempts())
                .filter(this::isNoneBlockedExceptLatest)
                .flatMap(this::validateLatestAttempt)
                .flatMap(this::validateAttemptsLimit)
                .switchIfEmpty(Mono.defer(() -> createOtpAttemptFor(cmId, identifierType, identifierValue, OtpRequestAttempt.AttemptStatus.SUCCESS, action)));
    }

    public Mono<Void> validateOTPRequest(String identifierType, String identifierValue, OtpRequestAttempt.Action action) {
        return validateOTPRequest(identifierType, identifierValue, action, "");
    }

    private Mono<Void> createOtpAttemptFor(String cmId, String identifierType, String identifierValue, OtpRequestAttempt.AttemptStatus attemptStatus, OtpRequestAttempt.Action action) {
        return otpRequestAttemptRepository.insert(cmId, identifierType, identifierValue, attemptStatus, action);
    }

    private Mono<List<OtpRequestAttempt>> validateLatestAttempt(List<OtpRequestAttempt> attempts) {
        OtpRequestAttempt latestAttempt = attempts.get(0);
        if (latestAttempt.getAttemptStatus() == OtpRequestAttempt.AttemptStatus.FAILURE) {
            return handleBlockedLatestAttempt(latestAttempt);
        }
        return Mono.just(attempts);
    }

    private Mono<List<OtpRequestAttempt>> handleBlockedLatestAttempt(OtpRequestAttempt latestAttempt) {
        boolean isInBlockingTime = isWithinTimeLimit(latestAttempt, userServiceProperties.getOtpAttemptsBlockPeriodInMin());
        if (isInBlockingTime) {
            return Mono.error(ClientError.otpRequestLimitExceeded());
        }
        return Mono.empty();
    }

    private Mono<Void> validateAttemptsLimit(List<OtpRequestAttempt> attempts) {
        OtpRequestAttempt firstAttempt = attempts.get(attempts.size() - 1);
        boolean isAttemptsLimitExceeded = isWithinTimeLimit(firstAttempt, userServiceProperties.getMaxOtpAttemptsPeriodInMin());
        if (isAttemptsLimitExceeded) {
            return createOtpAttemptFor(firstAttempt.getCmId(), firstAttempt.getIdentifierType(), firstAttempt.getIdentifierValue(), OtpRequestAttempt.AttemptStatus.FAILURE, firstAttempt.getAction())
                    .then(Mono.error(ClientError.otpRequestLimitExceeded()));
        }
        return Mono.empty();
    }

    private boolean isNoneBlockedExceptLatest(List<OtpRequestAttempt> otpRequestAttempts) {
        List<OtpRequestAttempt> duplicateAttempts = new ArrayList<>(otpRequestAttempts);
        duplicateAttempts.remove(0); //remove latest otp attempt
        return duplicateAttempts.stream().noneMatch(attempt -> attempt.getAttemptStatus() == OtpRequestAttempt.AttemptStatus.FAILURE);
    }

    public boolean isWithinTimeLimit(OtpRequestAttempt attempt, int timeLimitInMin) {
        return attempt
                .getAttemptAt()
                .plusMinutes(timeLimitInMin)
                .isAfter(LocalDateTime.now(ZoneOffset.UTC));
    }
}
