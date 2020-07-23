package in.projecteka.consentmanager.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
@Getter
public class VerifyAadharOtpResponse {
    private final String healthId;
    private final String token;
    private final PatientName name;
    private final DateOfBirth dateOfBirth;
    private final String gender;

}
