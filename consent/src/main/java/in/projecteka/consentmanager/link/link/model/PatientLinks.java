package in.projecteka.consentmanager.link.link.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@AllArgsConstructor
@Data
public class PatientLinks {
    private String id;
    private List<Links> links;
}
