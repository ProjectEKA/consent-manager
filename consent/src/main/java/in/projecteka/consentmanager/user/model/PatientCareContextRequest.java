package in.projecteka.consentmanager.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
@AllArgsConstructor
public class PatientCareContextRequest {
    private String hipId;
    private String patientId;
}
