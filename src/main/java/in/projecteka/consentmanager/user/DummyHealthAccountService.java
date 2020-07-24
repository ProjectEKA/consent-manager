package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.user.model.HASSignupRequest;
import in.projecteka.consentmanager.user.model.HealthAccountUser;
import in.projecteka.consentmanager.user.model.User;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@AllArgsConstructor
public class DummyHealthAccountService {

    private final UserRepository userRepository;

    public Mono<HealthAccountUser> createHASAccount(HASSignupRequest signupRequest) {
        return userRepository.getExistingUser(signupRequest.getFirstName(), signupRequest.getLastName(), signupRequest.getGender())
                .map(this::mapToHealthAccountUser)
                .switchIfEmpty(Mono.defer(() -> Mono.just(createNewDummyHASUser(signupRequest))));
    }

    private HealthAccountUser createNewDummyHASUser(HASSignupRequest signupRequest) {
        return HealthAccountUser.builder().firstName(signupRequest.getFirstName())
                .lastName(signupRequest.getLastName())
                .middleName(signupRequest.getMiddleName())
                .gender(signupRequest.getGender())
                .dayOfBirth(signupRequest.getDayOfBirth())
                .monthOfBirth(signupRequest.getMonthOfBirth())
                .yearOfBirth(signupRequest.getYearOfBirth())
                .newHASUser(true)
                .healthId(UUID.randomUUID().toString())
                .token(UUID.randomUUID().toString()).build();
    }

    private HealthAccountUser mapToHealthAccountUser(User user) {
        return HealthAccountUser.builder()
                .healthId(user.getHealthId())
                .token(UUID.randomUUID().toString())
                .firstName(user.getName().getFirst())
                .middleName(user.getName().getMiddle())
                .lastName(user.getName().getLast())
                .dayOfBirth(user.getDateOfBirth().getDate())
                .monthOfBirth(user.getDateOfBirth().getMonth())
                .yearOfBirth(user.getDateOfBirth().getYear())
                .gender(user.getGender().toString())
                .newHASUser(false)
                .build();
    }
}
