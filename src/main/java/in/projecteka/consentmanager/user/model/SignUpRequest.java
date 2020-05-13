package in.projecteka.consentmanager.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;

import java.util.List;

@Builder
@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class SignUpRequest {
    private final String name;
    private final String username;
    private final String password;
    private final Gender gender;
    private final Integer yearOfBirth;
    private final List<SignUpIdentifier> unverifiedIdentifiers;
}

