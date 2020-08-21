package in.projecteka.user.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class InitiateCmIdRecoveryRequest {
    private final PatientName name;
    private final Gender gender;
    private final DateOfBirth dateOfBirth;
    private final List<Identifier> verifiedIdentifiers;
    private final List<Identifier> unverifiedIdentifiers;
}
