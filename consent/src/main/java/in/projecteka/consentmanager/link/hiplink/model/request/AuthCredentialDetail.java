package in.projecteka.consentmanager.link.hiplink.model.request;

import lombok.Data;

@Data
public class AuthCredentialDetail {
    private final String token;
    private final DemographicDetail demographic;
}
