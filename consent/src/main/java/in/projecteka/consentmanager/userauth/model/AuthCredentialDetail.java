package in.projecteka.consentmanager.userauth.model;

import lombok.Data;

@Data
public class AuthCredentialDetail {
    private final String authCode;
    private final DemographicDetail demographic;
}
