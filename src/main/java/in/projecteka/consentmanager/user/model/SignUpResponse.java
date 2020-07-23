package in.projecteka.consentmanager.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class SignUpResponse {
    private final String token;
    private final String healthId;
    private final String cmId;
    private final PatientName patientName;
    private final DateOfBirth dateOfBirth;
    private final Gender gender;
    private final String stateName;
    private final String districtName;
    private final Boolean newHASUser;
}
