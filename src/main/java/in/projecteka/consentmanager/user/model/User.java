package in.projecteka.consentmanager.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class User {
    private String identifier;
    private String name;
    private Gender gender;
    private Integer yearOfBirth;
    private String phone;

    public static User from(SignUpRequest request, String mobileNumber) {
        return new User(request.getUserName().toLowerCase(),
                request.getName(),
                request.getGender(),
                request.getYearOfBirth(),
                mobileNumber);
    }
}
