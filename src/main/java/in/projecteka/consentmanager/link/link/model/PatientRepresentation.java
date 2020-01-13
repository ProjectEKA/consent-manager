package in.projecteka.consentmanager.link.link.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@AllArgsConstructor
@Builder
@NoArgsConstructor
@Setter
public class PatientRepresentation {
    private String referenceNumber;
    private String Display;
    private List<CareContextRepresentation> careContexts;
}
