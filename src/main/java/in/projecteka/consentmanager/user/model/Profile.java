package in.projecteka.consentmanager.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
@Builder
public class Profile {
    private final String id;
    private final String name;
    private final Gender gender;
    private final boolean hasTransactionPin;
    private final List<Identifier> verifiedIdentifiers;
    private final List<Identifier> unverifiedIdentifiers;
}
