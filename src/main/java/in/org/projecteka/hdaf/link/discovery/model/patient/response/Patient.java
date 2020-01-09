package in.org.projecteka.hdaf.link.discovery.model.patient.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Patient {
    private String referenceNumber;
    private String display;
    private List<CareContext> careContexts;
    private List<String> matchedBy;
}
