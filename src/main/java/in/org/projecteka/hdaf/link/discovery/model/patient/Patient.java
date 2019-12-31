package in.org.projecteka.hdaf.link.discovery.model.patient;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Patient {
    private String display;
}

class PatientRepresentation {
    private Patient patient;
}
