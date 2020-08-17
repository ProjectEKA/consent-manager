package in.projecteka.consentmanager.user.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum GrantType {
    PASSWORD("password"),
    REFRESH_TOKEN("refresh_token");

    private final String grantType;

    GrantType(String value) {
        grantType = value;
    }

    @JsonValue
    public String getValue() {
        return grantType;
    }
}
