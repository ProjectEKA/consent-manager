package in.projecteka.consentmanager.user.model;

import java.util.HashMap;

public enum IdentifierType {
    MOBILE {
        @Override
        public boolean isValid(String value) {
            return true;
        }
    },
    ABPMJAYID {
        @Override
        public boolean isValid(String abpmJayValue) {
            String regex = "^P[0-9,A-Z]{8}$";
            return abpmJayValue.matches(regex);
        }
    };

    public abstract boolean isValid(String value);

    public static final HashMap<IdentifierType, IdentifierGroup> identifierTypeGroupMap = new HashMap<>() {
        {
            put(IdentifierType.MOBILE, IdentifierGroup.VERIFIED_IDENTIFIER);
            put(IdentifierType.ABPMJAYID, IdentifierGroup.UNVERIFIED_IDENTIFIER);
        }
    };

    public IdentifierGroup getIdentifierGroup() {
        return identifierTypeGroupMap.get(this);
    }
}
