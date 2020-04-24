package in.projecteka.consentmanager.link.discovery.model.patient.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.validation.constraints.NotBlank;

import static in.projecteka.consentmanager.link.discovery.ValidationMessages.PATIENT_IDENTIFIER_TYPE_NOT_SPECIFIED;
import static in.projecteka.consentmanager.link.discovery.ValidationMessages.PATIENT_IDENTIFIER_VALUE_NOT_SPECIFIED;

@AllArgsConstructor
@Getter
public class PatientIdentifier {
    @NotBlank(message = PATIENT_IDENTIFIER_TYPE_NOT_SPECIFIED)
    private final PatientIdentifierType type;
    @NotBlank(message = PATIENT_IDENTIFIER_VALUE_NOT_SPECIFIED)
    private final String value;
}
