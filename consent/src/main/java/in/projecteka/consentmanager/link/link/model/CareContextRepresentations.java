package in.projecteka.consentmanager.link.link.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import in.projecteka.consentmanager.clients.model.CareContextRepresentation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@Builder
@NoArgsConstructor
@Data
public class CareContextRepresentations {
    private List<CareContextRepresentation> careContexts;
}