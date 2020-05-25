package in.projecteka.consentmanager.user.filters;

import in.projecteka.consentmanager.user.IdentifierUtils;
import in.projecteka.consentmanager.user.model.Identifier;
import in.projecteka.consentmanager.user.model.IdentifierType;
import in.projecteka.consentmanager.user.model.User;
import io.vertx.core.json.JsonArray;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ABPMJAYIdFilter implements FilterStrategy<List<Identifier>> {
    @Override
    public Mono<List<User>> filter(List<User> users, List<Identifier> identifiers) {
        if (IdentifierUtils.isIdentifierTypePresent(identifiers, IdentifierType.ABPMJAYID)) {
            String abpmjayId = IdentifierUtils.getIdentifierValue(identifiers, IdentifierType.ABPMJAYID);
            users = users.stream().filter(row -> isMatchingABPMJAYId(row.getUnverifiedIdentifiers(), abpmjayId)).collect(Collectors.toList());
        }
        return users.size() == 0 ? Mono.empty() : Mono.just(users);
    }

    private boolean isMatchingABPMJAYId(JsonArray unverifiedIdentifiers, String ABPMJAYId) {
        if (unverifiedIdentifiers == null || unverifiedIdentifiers.isEmpty()) {
            return false;
        }
        return IntStream.range(0, unverifiedIdentifiers.size())
                .mapToObj(unverifiedIdentifiers::getJsonObject)
                .anyMatch(identifier ->
                        identifier.getValue("type").equals(IdentifierType.ABPMJAYID.name())
                                && identifier.getValue("value").equals(ABPMJAYId)
                );
    }
}
