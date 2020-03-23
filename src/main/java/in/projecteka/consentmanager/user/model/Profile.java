package in.projecteka.consentmanager.user.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class Profile {
    private String id;
    private String firstName;
    private String lastName;
    private Gender gender;
    private boolean hasTransactionPin;
    private List<Identifier> verifiedIdentifiers;
    private List<Identifier> unverifiedIdentifiers;
}
