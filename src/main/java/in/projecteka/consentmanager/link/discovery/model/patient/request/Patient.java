package in.projecteka.consentmanager.link.discovery.model.patient.request;


import in.projecteka.consentmanager.clients.model.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

import java.util.Date;
import java.util.List;

@AllArgsConstructor
@Value
@Builder
@Data
public class Patient {
    private String id;
    private String firstName;
    private String lastName;
    private Gender gender;
    private Date dateOfBirth;
    private List<Identifier> verifiedIdentifiers;
    private List<Identifier> unVerifiedIdentifiers;
}
