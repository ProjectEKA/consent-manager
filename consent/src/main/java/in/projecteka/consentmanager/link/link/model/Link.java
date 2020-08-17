package in.projecteka.consentmanager.link.link.model;

import in.projecteka.consentmanager.clients.model.PatientRepresentation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@AllArgsConstructor
@Value
@Builder
public class Link {
    @NotNull
    String accessToken;
    @Valid
    PatientRepresentation patient;
}