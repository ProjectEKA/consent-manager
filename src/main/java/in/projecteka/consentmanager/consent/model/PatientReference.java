package in.projecteka.consentmanager.consent.model;

import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

@Value
@Builder
public class PatientReference implements Serializable {
    @NotEmpty(message = "Patient identifier is not specified.")
    private String id;
}
