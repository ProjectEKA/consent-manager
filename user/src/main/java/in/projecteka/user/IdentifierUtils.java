package in.projecteka.user;

import in.projecteka.user.model.Identifier;
import in.projecteka.user.model.IdentifierGroup;
import in.projecteka.user.model.IdentifierType;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static in.projecteka.user.model.IdentifierGroup.UNVERIFIED_IDENTIFIER;
import static in.projecteka.user.model.IdentifierGroup.VERIFIED_IDENTIFIER;
import static in.projecteka.user.model.IdentifierType.ABPMJAYID;
import static in.projecteka.user.model.IdentifierType.MOBILE;

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
                .collect(Collectors.toList())
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
