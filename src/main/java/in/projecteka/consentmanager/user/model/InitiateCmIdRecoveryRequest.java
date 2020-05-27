package in.projecteka.consentmanager.user.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;


@AllArgsConstructor
@Getter
public class InitiateCmIdRecoveryRequest {
    private final String name;
    private final Gender gender;
    private final Integer yearOfBirth;
    private final List<Identifier> verifiedIdentifiers;
    private final List<Identifier> unverifiedIdentifiers;
}
