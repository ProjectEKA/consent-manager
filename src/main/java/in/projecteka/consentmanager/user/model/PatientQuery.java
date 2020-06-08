package in.projecteka.consentmanager.user.model;

import in.projecteka.consentmanager.consent.model.PatientReference;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PatientQuery {
    private PatientReference patient;
    private RequesterDetail requester;
}