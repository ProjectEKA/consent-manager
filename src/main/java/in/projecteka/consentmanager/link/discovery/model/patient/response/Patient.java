package in.projecteka.consentmanager.link.discovery.model.patient.response;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class Patient {
    String referenceNumber;
    String display;
    List<CareContext> careContexts;
    List<String> matchedBy;
}
