package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.user.model.Identifier;
import in.projecteka.consentmanager.user.model.IdentifierType;
import in.projecteka.consentmanager.user.model.User;
import io.vertx.core.json.JsonArray;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ABPMJAYIdFilter implements FilterStrategy<List<Identifier>>{
    @Override
    public Mono<List<User>> filter(List<User> users, List<Identifier> identifiers) {
        if (isIdentifierTypePresent(identifiers, IdentifierType.ABPMJAYID)) {
            String ABPMJAYId = getIdentifierValue(identifiers, IdentifierType.ABPMJAYID);
            System.out.println(ABPMJAYId + " abpmjayId");
            users = users.stream().filter(row -> isMatchingABPMJAYId(row.getUnverifiedIdentifiers(), ABPMJAYId)).collect(Collectors.toList());
        }
        System.out.println(users.size() + " filtered size");
        return users.size() == 0 ? Mono.empty() : Mono.just(users);
    }

    private boolean isMatchingABPMJAYId(JsonArray unverifiedIdentifiers, String ABPMJAYId) {
        return IntStream.range(0, unverifiedIdentifiers.size())
                .mapToObj(unverifiedIdentifiers::getJsonObject)
                .anyMatch(identifier ->
                        identifier.getValue("type").equals(IdentifierType.ABPMJAYID.name())
                                && identifier.getValue("value").equals(ABPMJAYId)
                );
    }

    static String getIdentifierValue(List<Identifier> identifiers, IdentifierType type) {
        return identifiers.stream().filter(identifier -> isIdentifierType(identifier, type)).collect(Collectors.toList()).get(0).getValue();
    }

    static boolean isIdentifierType(Identifier identifier, IdentifierType type) {
        return identifier.getType().equals(type);
    }

    static boolean isIdentifierTypePresent(List<Identifier> identifiers, IdentifierType type) {
        return identifiers.stream().anyMatch(identifier -> isIdentifierType(identifier, type));
    }
}
