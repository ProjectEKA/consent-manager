package in.projecteka.consentmanager.user.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class CoreSignUpRequest {
    private final String name;
    private final String username;
    private final String password;
    private final Gender gender;
    private final Integer yearOfBirth;
    private final List<Identifier> unverifiedIdentifiers;
}
