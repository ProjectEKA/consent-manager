package in.projecteka.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
@AllArgsConstructor
public class PatientCareContextRequest {
    private final String hipId;
    private final String patientId;
}
