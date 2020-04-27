package in.projecteka.consentmanager.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

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
}