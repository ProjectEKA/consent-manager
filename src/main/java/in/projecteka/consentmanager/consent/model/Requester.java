package in.projecteka.consentmanager.consent.model;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;

@Getter
@Setter
public class Requester {
    @NotEmpty(message = "Requester name is not specified.")
    private String name;
    private Identifier identifier;
}
