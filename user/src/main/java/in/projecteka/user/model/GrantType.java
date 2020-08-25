package in.projecteka.user.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum GrantType {
    PASSWORD("password"),
    REFRESH_TOKEN("refresh_token");

    private final String type;

    GrantType(String value) {
        type = value;
    }

    @JsonValue
    public String getValue() {
        return type;
    }
}
