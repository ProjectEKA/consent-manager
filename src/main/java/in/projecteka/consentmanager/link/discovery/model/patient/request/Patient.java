package in.projecteka.consentmanager.link.discovery.model.patient.request;

import in.projecteka.consentmanager.clients.model.Gender;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Builder
@Value
public class Patient {
    String id;
    String name;
    Gender gender;
    Integer yearOfBirth;
    List<Identifier> verifiedIdentifiers;
    List<Identifier> unverifiedIdentifiers;
}
