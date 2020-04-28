package in.projecteka.consentmanager.link.discovery.model.patient.request;

import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotBlank;

import static in.projecteka.consentmanager.link.discovery.ValidationMessages.PATIENT_IDENTIFIER_TYPE_NOT_SPECIFIED;
import static in.projecteka.consentmanager.link.discovery.ValidationMessages.PATIENT_IDENTIFIER_VALUE_NOT_SPECIFIED;

@Value
@Builder
public class PatientIdentifier {
    @NotBlank(message = PATIENT_IDENTIFIER_TYPE_NOT_SPECIFIED) PatientIdentifierType type;
    @NotBlank(message = PATIENT_IDENTIFIER_VALUE_NOT_SPECIFIED) String value;
}
