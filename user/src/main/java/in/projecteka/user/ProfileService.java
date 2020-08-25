package in.projecteka.user;

import in.projecteka.user.model.Identifier;
import in.projecteka.user.model.IdentifierType;
import in.projecteka.user.model.Profile;
import in.projecteka.user.model.User;
import io.vertx.core.json.JsonArray;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
        Profile.ProfileBuilder builder = Profile.builder()
                .id(user.getIdentifier())
                .name(user.getName())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .hasTransactionPin(hasTransactionPin)
                .verifiedIdentifiers(singletonList(new Identifier(IdentifierType.MOBILE, user.getPhone())));
        JsonArray unverifiedIdentifiersJson = user.getUnverifiedIdentifiers();
        if (unverifiedIdentifiersJson != null) {
            List<Identifier> unverifiedIdentifiers = IntStream.range(0, user.getUnverifiedIdentifiers().size())
                    .mapToObj(unverifiedIdentifiersJson::getJsonObject)
                    .map(jsonObject -> new Identifier(IdentifierType.valueOf(jsonObject.getString("type")), jsonObject.getString("value")))
                    .collect(Collectors.toList());
            builder.unverifiedIdentifiers(unverifiedIdentifiers);
        }
        return builder.build();
    }
}
