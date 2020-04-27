package in.projecteka.consentmanager.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class User {
    private final String identifier;
    private final String name;
    private final Gender gender;
    private final Integer yearOfBirth;
    private final String phone;

    public static User from(SignUpRequest request, String mobileNumber) {
        return new User(request.getUsername().toLowerCase(),
                request.getName(),
                request.getGender(),
                request.getYearOfBirth(),
                mobileNumber);
    }
}
