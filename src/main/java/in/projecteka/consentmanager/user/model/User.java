package in.projecteka.consentmanager.user.model;

import io.vertx.core.json.JsonArray;
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
    private final JsonArray unverifiedIdentifiers;

    public static User from(CoreSignUpRequest request, String mobileNumber) {
        return new User(request.getUsername().toLowerCase(),
                request.getName(),
                request.getGender(),
                request.getYearOfBirth(),
                mobileNumber,
                request.getUnverifiedIdentifiers() != null ? new JsonArray(request.getUnverifiedIdentifiers()) : null);
    }
}
