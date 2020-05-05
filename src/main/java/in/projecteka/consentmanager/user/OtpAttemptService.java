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

    public Mono<Void> createOtpAttemptFor(String phoneNumber, boolean blockedStatus) {
        return otpAttemptRepository.insert(phoneNumber, blockedStatus);
    }


    public Mono<Void> validateOTPRequest(String phoneNumber) {
        return otpAttemptRepository.getOtpAttempts(phoneNumber, userServiceProperties.getMaxOtpAttempts())
                .filter(otpAttempts -> otpAttempts.size() >= userServiceProperties.getMaxOtpAttempts())
                .filter(this::isNoneBlockedExceptLatest)
                .flatMap(this::validateLatestAttempt)
                .flatMap(this::validateAttemptsLimit)
                .switchIfEmpty(createOtpAttemptFor(phoneNumber, false));
    }

    private Mono<List<OtpAttempt>> validateLatestAttempt(List<OtpAttempt> attempts) {
        OtpAttempt latestAttempt = attempts.get(0);
        if (latestAttempt.isBlocked()) {
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
            return createOtpAttemptFor(firstAttempt.getPhoneNumber(), true)
                    .then(Mono.error(ClientError.otpRequestLimitExceeded()));
        }
        return Mono.empty();
    }

    private boolean isNoneBlockedExceptLatest(List<OtpAttempt> attempts) {
        List<OtpAttempt> duplicateAttempts = new ArrayList<>(attempts);
        duplicateAttempts.remove(0); //remove latest otp attempt
        return duplicateAttempts.stream().noneMatch(OtpAttempt::isBlocked);
    }

    public boolean isWithinTimeLimit(OtpAttempt attempt, int timeLimitInMin) {
        return attempt
                .getTimestamp()
                .plusMinutes(timeLimitInMin)
                .isAfter(LocalDateTime.now(ZoneOffset.UTC));
    }
}
