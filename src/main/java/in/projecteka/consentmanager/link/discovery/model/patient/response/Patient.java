package in.projecteka.consentmanager.link.discovery.model.patient.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Patient {
    private String referenceNumber;
    private String display;
    private List<CareContext> careContexts;
    private List<String> matchedBy;
}
