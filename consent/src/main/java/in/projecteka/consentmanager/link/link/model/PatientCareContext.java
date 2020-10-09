package in.projecteka.consentmanager.link.link.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Builder
@AllArgsConstructor
@Data
@NoArgsConstructor
public class PatientCareContext implements Serializable {
    private String patientReference;
    private String careContextReference;
}
