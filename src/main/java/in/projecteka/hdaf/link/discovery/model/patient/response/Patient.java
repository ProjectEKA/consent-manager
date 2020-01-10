package in.projecteka.hdaf.link.discovery.model.patient.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Patient {
    private String referenceNumber;
    private String display;
    private List<CareContext> careContexts;
    private List<String> matchedBy;
}
