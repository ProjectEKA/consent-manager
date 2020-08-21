package in.projecteka.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class SignUpRequest {
    private final PatientName name;
    private final String username;
    private final String password;
    private final Gender gender;
    private final DateOfBirth dateOfBirth;
    private final List<SignUpIdentifier> unverifiedIdentifiers;
}

