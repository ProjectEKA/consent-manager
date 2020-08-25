package in.projecteka.consentmanager.clients.model;

import in.projecteka.library.clients.model.DateOfBirth;
import in.projecteka.library.clients.model.PatientName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class User {
    private String identifier;
    private PatientName name;
    private Gender gender;
    private DateOfBirth dateOfBirth;
    private String phone;
}
