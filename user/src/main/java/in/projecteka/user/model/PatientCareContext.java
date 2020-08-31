package in.projecteka.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
@AllArgsConstructor
public class PatientCareContext {
    private final String patientReference;
    private final String careContextReference;
}
