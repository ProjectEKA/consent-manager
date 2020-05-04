package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.user.model.OtpAttempt;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@AllArgsConstructor
public class OtpAttemptService {

    private final OtpAttemptRepository otpAttemptRepository;
    private final UserServiceProperties userServiceProperties;

    public Mono<Void> createOtpAttemptFor(String phoneNumber, boolean blockedStatus) {
        return otpAttemptRepository.insert(phoneNumber, blockedStatus);
    }


    public Mono<Void> validateOTPRequest(String phoneNumber) {
        return otpAttemptRepository.select(phoneNumber, userServiceProperties.getMaxOtpAttempts())
                .filter(otpAttempts -> otpAttempts.size() >= userServiceProperties.getMaxOtpAttempts())
                .flatMap(attempts -> {
                    OtpAttempt latestAttempt = attempts.get(0);
                    if (latestAttempt.isBlocked()) {
                        boolean isInBlockingTime = isWithinTimeLimit(latestAttempt, userServiceProperties.getOtpAttemptsBlockPeriodInMin());
                        if (isInBlockingTime) {
                            return Mono.error(ClientError.otpRequestLimitExceeded());
                        }
                        return Mono.empty();
                    }
                    if (attempts.stream().anyMatch(OtpAttempt::isBlocked)) {
                        return Mono.empty();
                    }
                    boolean isAttemptsLimitExceeded = isWithinTimeLimit(
                            attempts.get(attempts.size() - 1),
                            userServiceProperties.getMaxOtpAttemptsPeriodInMin());
                    if (isAttemptsLimitExceeded) {
                        return createOtpAttemptFor(phoneNumber, true)
                                .then(Mono.error(ClientError.otpRequestLimitExceeded()));
                    }
                    return Mono.empty();
                }).then(createOtpAttemptFor(phoneNumber, false));
    }

    public boolean isWithinTimeLimit(OtpAttempt attempt, int timeLimit) {
        return attempt
                .getTimestamp()
                .plusMinutes(timeLimit)
                .isAfter(LocalDateTime.now(ZoneOffset.UTC));
    }
}
