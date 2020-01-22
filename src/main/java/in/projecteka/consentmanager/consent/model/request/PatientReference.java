package in.projecteka.consentmanager.consent.model.request;

import lombok.*;

import javax.validation.constraints.NotEmpty;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PatientReference {
    @NotEmpty(message = "Patient identifier is not specified.")
    private String id;
}
