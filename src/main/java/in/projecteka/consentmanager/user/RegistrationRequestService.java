package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.user.model.RegistrationRequest;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class RegistrationRequestService {

    private final RegistrationRequestRepository registrationRequestRepository;

    public Mono<Void> createRegistrationRequestFor(String phoneNumber, boolean blockedStatus) {
        return registrationRequestRepository.insert(phoneNumber, blockedStatus);
    }


    public Mono<Void> validateOTPRequest(String phoneNumber) {
        return registrationRequestRepository.select(phoneNumber)
                .flatMap(requests -> {
                    if(requests.isEmpty()){
                        return Mono.empty();
                    }
                    RegistrationRequest lastRequest = requests.get(requests.size() - 1);
                    if (lastRequest.isBlockedStatus()) {
                        return Mono.error(ClientError.otpRequestLimitExceeded());
                    }
                    if (requests.size() >= 5) {
                        return createRegistrationRequestFor(phoneNumber, true)
                                .then(Mono.error(ClientError.otpRequestLimitExceeded()));
                    }
                    return Mono.empty();
                }).then(createRegistrationRequestFor(phoneNumber, false));
    }

}
