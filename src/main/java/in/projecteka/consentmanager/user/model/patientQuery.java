package in.projecteka.consentmanager.user.model;

import in.projecteka.consentmanager.consent.model.PatientReference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class patientQuery {
    private PatientReference patient;
    private RequesterDetail requester;
}