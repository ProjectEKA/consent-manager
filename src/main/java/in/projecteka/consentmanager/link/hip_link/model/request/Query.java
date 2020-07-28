package in.projecteka.consentmanager.link.hip_link.model.request;

import lombok.Value;

@Value
public class Query {
    Patient patient;
    String purpose;
    Requester requester;
}
