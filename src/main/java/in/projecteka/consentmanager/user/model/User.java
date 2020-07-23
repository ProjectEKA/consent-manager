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
    private final PatientName name;
    private final Gender gender;
    private final DateOfBirth dateOfBirth;
    private final String phone;
    private final JsonArray unverifiedIdentifiers;
    private final String healthId;

    public static User from(CoreSignUpRequest request, String mobileNumber) {
        return new User(request.getUsername().toLowerCase(),
                request.getName(),
                request.getGender(),
                request.getDateOfBirth(),
                mobileNumber,
                request.getUnverifiedIdentifiers() != null ? new JsonArray(request.getUnverifiedIdentifiers()) : null,
                null
                );
    }
}
