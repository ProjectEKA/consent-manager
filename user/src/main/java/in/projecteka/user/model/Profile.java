package in.projecteka.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
@Builder
public class Profile {
    private final String id;
    private final PatientName name;
    private final Gender gender;
    private final DateOfBirth dateOfBirth;
    private final boolean hasTransactionPin;
    private final List<Identifier> verifiedIdentifiers;
    private final List<Identifier> unverifiedIdentifiers;
}
