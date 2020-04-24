package in.projecteka.consentmanager.link.discovery.model.patient.request;

import in.projecteka.consentmanager.clients.model.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Builder
@Getter
@EqualsAndHashCode
public class Patient {
    String id;
    String name;
    Gender gender;
    Integer yearOfBirth;
    List<Identifier> verifiedIdentifiers;
    List<Identifier> unverifiedIdentifiers;
}
