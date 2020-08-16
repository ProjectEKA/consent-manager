package in.projecteka.consentmanager.user.model;

import in.projecteka.consentmanager.user.IdentifierUtils;

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

    public IdentifierGroup getIdentifierGroup() {
        return IdentifierUtils.identifierTypeGroupMap.get(this);
    }
}
