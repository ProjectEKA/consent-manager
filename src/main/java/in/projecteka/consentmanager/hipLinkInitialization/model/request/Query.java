package in.projecteka.consentmanager.hipLinkInitialization.model.request;

import lombok.Value;

@Value
public class Query {
    Patient patient;
    String purpose;
    Requester requester;
}
