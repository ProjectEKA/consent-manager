package in.projecteka.consentmanager.consent.model;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

@Data
public class Requester implements Serializable {
    @NotEmpty(message = "Requester name is not specified.")
    private String name;
    private Identifier identifier;
}
