package in.projecteka.consentmanager.user.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Profile {
    private String id;
    private String firstName;
    private String lastName;
    private Gender gender;
    private boolean hasTransactionPin;
}
