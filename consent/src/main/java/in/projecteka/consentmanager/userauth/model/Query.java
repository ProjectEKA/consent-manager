package in.projecteka.consentmanager.userauth.model;

import lombok.NonNull;
import lombok.Value;

import javax.validation.Valid;

@Value
public class Query {
    @Valid
    @NonNull
    String id;
    AuthPurpose purpose;
    AuthMode authMode;
    @Valid
    @NonNull
    Requester requester;
}
