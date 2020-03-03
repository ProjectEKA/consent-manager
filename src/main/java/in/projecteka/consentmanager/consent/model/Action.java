package in.projecteka.consentmanager.consent.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Action {
    CONSENT_REQUEST_CREATED("ConsentRequestCreated");

    private final String resourceType;
    Action(String value) {
        resourceType = value;
    }

    @JsonValue
    public String getValue() {
        return resourceType;
    }
}
