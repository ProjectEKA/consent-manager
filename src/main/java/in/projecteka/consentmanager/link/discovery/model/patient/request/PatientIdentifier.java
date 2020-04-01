package in.projecteka.consentmanager.link.discovery.model.patient.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientIdentifier {
    @NotBlank(message = "Identifier Type must be specified")
    private PatientIdentifierType type;
    @NotBlank(message = "Identifier Value must be specified")
    private String value;
}
