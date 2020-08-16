package in.projecteka.consentmanager.clients.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Action {
    CONSENT_REQUEST_CREATED("ConsentRequestCreated"),
    CONSENT_MANAGER_ID_RECOVERED("ConsentManagerIdRecovered");

    private final String resourceType;

    Action(String value) {
        resourceType = value;
    }

    @JsonValue
    public String getValue() {
        return resourceType;
    }
}
