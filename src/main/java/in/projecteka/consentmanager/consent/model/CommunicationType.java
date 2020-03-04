package in.projecteka.consentmanager.consent.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum CommunicationType {
    MOBILE("Mobile");

    private final String resourceType;
    CommunicationType(String value) {
        resourceType = value;
    }

    @JsonValue
    public String getValue() {
        return resourceType;
    }
}
