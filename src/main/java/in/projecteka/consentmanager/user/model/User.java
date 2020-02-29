package in.projecteka.consentmanager.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class User {
    private String identifier;
    private String firstName;
    private String lastName;
    private Gender gender;
    private LocalDate dateOfBirth;
    private String phone;

    public static User from(SignUpRequest request, String mobileNumber) {
        return new User(request.getUserName(),
                request.getFirstName(),
                request.getLastName(),
                request.getGender(),
                request.getDateOfBirth(),
                mobileNumber);
    }
}
