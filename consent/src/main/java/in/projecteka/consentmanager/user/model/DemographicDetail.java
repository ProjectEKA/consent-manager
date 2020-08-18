package in.projecteka.consentmanager.user.model;

import lombok.Data;

@Data
public class DemographicDetail {
    private final String name;
    private final Gender gender;
    private final String dateOfBirth;
    private final Identifier identifier;
}
