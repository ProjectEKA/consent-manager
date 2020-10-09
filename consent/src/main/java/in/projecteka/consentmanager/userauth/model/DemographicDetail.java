package in.projecteka.consentmanager.userauth.model;

import in.projecteka.library.clients.model.Gender;
import lombok.Data;

@Data
public class DemographicDetail {
    private final String name;
    private final Gender gender;
    private final String dateOfBirth;
    private final Identifier identifier;
}
