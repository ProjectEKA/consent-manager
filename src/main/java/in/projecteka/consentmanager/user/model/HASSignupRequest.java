package in.projecteka.consentmanager.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class HASSignupRequest {
    private final String firstName;
    private final String middleName;
    private final String lastName;
    private final String name;
    private final String gender;
    private final Integer dayOfBirth;
    private final Integer monthOfBirth;
    private final Integer yearOfBirth;
    private final String token;
}
