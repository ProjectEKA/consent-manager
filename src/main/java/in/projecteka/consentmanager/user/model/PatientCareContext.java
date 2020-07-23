package in.projecteka.consentmanager.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
@AllArgsConstructor
public class PatientCareContext {
    private String patientReference;
    private String careContextReference;
}
