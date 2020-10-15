package in.projecteka.consentmanager.userauth.model;

import java.util.List;
import java.util.Map;

import static in.projecteka.consentmanager.userauth.model.IdentifierGroup.UNVERIFIED_IDENTIFIER;
import static in.projecteka.consentmanager.userauth.model.IdentifierGroup.VERIFIED_IDENTIFIER;
import static in.projecteka.consentmanager.userauth.model.IdentifierType.ABPMJAYID;
import static in.projecteka.consentmanager.userauth.model.IdentifierType.MOBILE;
import static java.util.stream.Collectors.toList;

public class IdentifierUtils {

    public static final Map<IdentifierType, IdentifierGroup> identifierTypeGroupMap =
            Map.of(MOBILE, VERIFIED_IDENTIFIER,
                    ABPMJAYID, UNVERIFIED_IDENTIFIER);

    private IdentifierUtils() {
    }

    /*
     TODO:
     this is brittle, you may not have the identifier-type you are looking for always.
     need to be fixed, and return optional<String>
     and caller make a decision accordingly
    */
    public static String getIdentifierValue(List<Identifier> identifiers, IdentifierType type) {
        return identifiers.stream()
                .filter(identifier -> isIdentifierType(identifier, type))
                .collect(toList())
                .get(0)
                .getValue();
    }

    static boolean isIdentifierType(Identifier identifier, IdentifierType type) {
        return identifier.getType().equals(type);
    }

    public static boolean isIdentifierTypePresent(List<Identifier> identifiers, IdentifierType type) {
        return identifiers.stream().anyMatch(identifier -> isIdentifierType(identifier, type));
    }
}