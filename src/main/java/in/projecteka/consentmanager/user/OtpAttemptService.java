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
                .flatMap(attempts -> {
                    if (attempts.size() < 5) {
                        return Mono.empty();
                    }
                    OtpAttempt latestAttempt = attempts.get(0);
                    if (latestAttempt.isBlocked()) {
                        boolean isInBlockingTime = latestAttempt
                                .getTimestamp()
                                .plusMinutes(userServiceProperties.getOtpAttemptsBlockPeriodInMin())
                                .isAfter(LocalDateTime.now(ZoneOffset.UTC));
                        if (isInBlockingTime) {
                            return Mono.error(ClientError.otpRequestLimitExceeded());
                        }
                        return Mono.empty();
                    }
                    boolean isValidAttempt = attempts.stream().anyMatch(OtpAttempt::isBlocked);
                    if(isValidAttempt) {
                        return Mono.empty();
                    }
                    OtpAttempt firstAttempt = attempts.get(attempts.size() - 1);
                    boolean isAttemptsLimitExceeded = firstAttempt
                            .getTimestamp()
                            .plusMinutes(userServiceProperties.getMaxOtpAttemptsPeriodInMin())
                            .isAfter(LocalDateTime.now(ZoneOffset.UTC));
                    if (isAttemptsLimitExceeded) {
                        return createOtpAttemptFor(phoneNumber, true)
                                .then(Mono.error(ClientError.otpRequestLimitExceeded()));
                    }
                    return Mono.empty();
                }).then(createOtpAttemptFor(phoneNumber, false));
    }
}
