package in.projecteka.consentmanager.consent.model;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

@Getter
@Setter
public class Requester implements Serializable {
    @NotEmpty(message = "Requester name is not specified.")
    private String name;
    private Identifier identifier;
}
