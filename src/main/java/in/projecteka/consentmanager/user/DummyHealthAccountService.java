package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.user.model.GenerateAadharOtpResponse;
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
        return Mono.just(createNewDummyHASUser(signupRequest));
//        return userRepository.getExistingUser(signupRequest.getFirstName(), signupRequest.getLastName(), signupRequest.getGender())
//                .flatMap(user -> Mono.just(mapToHealthAccountUser(user)))
//                .switchIfEmpty(Mono.defer(() -> Mono.just(createNewDummyHASUser(signupRequest))));
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

    public GenerateAadharOtpResponse createDummyGenerateAadharOtpResponse(String token) {
        return GenerateAadharOtpResponse.builder()
                .txnID(UUID.randomUUID().toString())
                .token(token)
                .build();
    }

    public HealthAccountUser createHASUser() {
        return HealthAccountUser.builder()
                .name("Hina Patel")
                .firstName("Hina")
                .middleName("")
                .lastName("Patel")
                .gender("F")
                .dayOfBirth(12)
                .monthOfBirth(12)
                .yearOfBirth(1979)
                .newHASUser(true)
                .healthId(UUID.randomUUID().toString())
                .token(UUID.randomUUID().toString()).build();
    }

    public HealthAccountUser mapToHealthAccountUser(User user) {
        return mapToHealthAccountUser(user,false);
    }

    public HealthAccountUser mapToHealthAccountUser(User user, Boolean newHASUser) {
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
                .districtName("Pune")
                .stateName("Maharashtra")
                .newHASUser(newHASUser)
                .build();
    }
}
