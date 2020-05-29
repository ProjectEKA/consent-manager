package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.user.model.Identifier;
import in.projecteka.consentmanager.user.model.IdentifierGroup;
import in.projecteka.consentmanager.user.model.IdentifierType;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class IdentifierUtils {

    public static String getIdentifierValue(List<Identifier> identifiers, IdentifierType type) {
        return identifiers.stream().filter(identifier -> isIdentifierType(identifier, type)).collect(Collectors.toList()).get(0).getValue();
    }

    static boolean isIdentifierType(Identifier identifier, IdentifierType type) {
        return identifier.getType().equals(type);
    }

    public static boolean isIdentifierTypePresent(List<Identifier> identifiers, IdentifierType type) {
        return identifiers.stream().anyMatch(identifier -> isIdentifierType(identifier, type));
    }

    public static final HashMap<IdentifierType, IdentifierGroup> identifierTypeGroupMap = new HashMap<>() {
        {
            put(IdentifierType.MOBILE, IdentifierGroup.VERIFIED_IDENTIFIER);
            put(IdentifierType.ABPMJAYID, IdentifierGroup.UNVERIFIED_IDENTIFIER);
        }
    };
}
