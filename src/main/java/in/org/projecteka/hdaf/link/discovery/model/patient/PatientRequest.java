package in.org.projecteka.hdaf.link.discovery.model.patient;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;


import java.util.List;

@AllArgsConstructor
@Getter
@Value
public class PatientRequest {
    private String firstName;
    private List<Identifier> verifiedIdentifiers;
}

