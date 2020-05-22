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

    public static final HashMap<IdentifierType, BroaderIdentifierType> identifierTypes = new HashMap<>() {
        {
            put(IdentifierType.MOBILE, BroaderIdentifierType.VERIFIED_IDENTIFIER);
            put(IdentifierType.ABPMJAYID, BroaderIdentifierType.UNVERIFIED_IDENTIFIER);
        }
    };

    public BroaderIdentifierType getBroaderIdentifierType() {
        return identifierTypes.get(this);
    }
}
