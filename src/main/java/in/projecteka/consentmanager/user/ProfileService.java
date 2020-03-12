package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.user.model.Profile;
import in.projecteka.consentmanager.user.model.User;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class ProfileService {
    private UserService userService;
    private TransactionPinService transactionPinService;

    public Mono<Profile> getProfileFor(String patientId) {
        return userService.userWith(patientId)
                .flatMap(user -> transactionPinService.isTransactionPinSet(patientId)
                        .map(hasTransactionPin -> profileFrom(user, hasTransactionPin)));
    }

    private Profile profileFrom(User user, Boolean hasTransactionPin) {
        return Profile.builder()
                .id(user.getIdentifier())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .gender(user.getGender())
                .hasTransactionPin(hasTransactionPin)
                .build();
    }
}
