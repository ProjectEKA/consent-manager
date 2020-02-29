package in.projecteka.consentmanager.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Builder
@Data
@AllArgsConstructor
public class SignUpRequest {
    private String firstName;
    private String lastName;
    private String userName;
    private String password;
    private Gender gender;
    private LocalDate dateOfBirth;
}