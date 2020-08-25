package in.projecteka.user.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class CoreSignUpRequest {
    PatientName name;
    String username;
    String password;
    Gender gender;
    DateOfBirth dateOfBirth;
    List<Identifier> unverifiedIdentifiers;
}
