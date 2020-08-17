package in.projecteka.consentmanager.link.link.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import in.projecteka.consentmanager.clients.model.PatientRepresentation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor
public class Links {
    private Hip hip;
    @JsonUnwrapped
    private PatientRepresentation patientRepresentations;
}
