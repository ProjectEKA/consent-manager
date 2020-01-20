package in.projecteka.consentmanager.consent.model.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Requester {
    private String name;
    private Identifier identifier;
}
