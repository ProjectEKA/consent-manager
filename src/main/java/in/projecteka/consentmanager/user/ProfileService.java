package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.user.model.Identifier;
import in.projecteka.consentmanager.user.model.IdentifierType;
import in.projecteka.consentmanager.user.model.Profile;
import in.projecteka.consentmanager.user.model.User;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

import static java.util.Collections.singletonList;

@AllArgsConstructor
public class ProfileService {
    private final UserService userService;
    private final TransactionPinService transactionPinService;

    public Mono<Profile> profileFor(String patientId) {
        return userService.userWith(patientId)
                .flatMap(user -> transactionPinService.isTransactionPinSet(patientId)
                        .map(hasTransactionPin -> from(user, hasTransactionPin)));
    }

    private Profile from(User user, Boolean hasTransactionPin) {
        return Profile.builder()
                .id(user.getIdentifier())
                .name(user.getName())
                .gender(user.getGender())
                .hasTransactionPin(hasTransactionPin)
                .verifiedIdentifiers(singletonList(new Identifier(IdentifierType.MOBILE, user.getPhone())))
                .build();
    }
}
