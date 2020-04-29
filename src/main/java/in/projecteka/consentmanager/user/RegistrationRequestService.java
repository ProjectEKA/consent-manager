package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.user.model.RegistrationRequest;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class RegistrationRequestService {

    private final RegistrationRequestRepository registrationRequestRepository;

    public Mono<Void> createRegistrationRequestFor(String phoneNumber) {
        RegistrationRequest registrationRequest = new RegistrationRequest(phoneNumber);
        return registrationRequestRepository.insert(registrationRequest);
    }
}
