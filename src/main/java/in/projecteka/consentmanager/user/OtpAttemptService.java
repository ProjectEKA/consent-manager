package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.user.model.OtpAttempt;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class OtpAttemptService {

    private final OtpAttemptRepository otpAttemptRepository;

    public Mono<Void> createOtpAttemptFor(String phoneNumber, boolean blockedStatus) {
        return otpAttemptRepository.insert(phoneNumber, blockedStatus);
    }


    public Mono<Void> validateOTPRequest(String phoneNumber) {
        return otpAttemptRepository.select(phoneNumber)
                .flatMap(requests -> {
                    if(requests.isEmpty()){
                        return Mono.empty();
                    }
                    OtpAttempt lastRequest = requests.get(requests.size() - 1);
                    if (lastRequest.isBlockedStatus()) {
                        return Mono.error(ClientError.otpRequestLimitExceeded());
                    }
                    if (requests.size() >= 5) {
                        return createOtpAttemptFor(phoneNumber, true)
                                .then(Mono.error(ClientError.otpRequestLimitExceeded()));
                    }
                    return Mono.empty();
                }).then(createOtpAttemptFor(phoneNumber, false));
    }
}
