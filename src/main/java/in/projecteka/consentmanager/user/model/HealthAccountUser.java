package in.projecteka.consentmanager.user.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class HealthAccountUser {
    private final String healthIdNumber;
    private final String firstName;
    private final String middleName;
    private final String lastName;
    private final String name;
    private final String gender;
    private final Integer dayOfBirth;
    private final Integer monthOfBirth;
    private final Integer yearOfBirth;
    private final String token;
    private final String districtName;
    private final String stateName;
    @JsonProperty("new")
    private final Boolean newHASUser;
    private final String stateCode;
    private final String districtCode;
}
