package in.projecteka.consentmanager.link.discovery.model.patient.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

import static in.projecteka.consentmanager.link.discovery.ValidationMessages.PATIENT_IDENTIFIER_TYPE_NOT_SPECIFIED;
import static in.projecteka.consentmanager.link.discovery.ValidationMessages.PATIENT_IDENTIFIER_VALUE_NOT_SPECIFIED;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientIdentifier {
    @NotBlank(message = PATIENT_IDENTIFIER_TYPE_NOT_SPECIFIED)
    private PatientIdentifierType type;
    @NotBlank(message = PATIENT_IDENTIFIER_VALUE_NOT_SPECIFIED)
    private String value;
}
