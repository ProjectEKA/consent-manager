package in.projecteka.consentmanager.user.model;

import lombok.Data;

@Data
public class AuthCredentialDetail {
    private final String token;
    private final DemographicDetail demographic;
}

