package in.projecteka.consentmanager.clients.model;

import in.projecteka.consentmanager.link.discovery.model.Gender;
import in.projecteka.consentmanager.link.discovery.model.Phone;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class User {
    private String identifier;
    private String firstName;
    private String lastName;
    private Gender gender;
    private Date dateOfBirth;
    private Phone phone;
}
