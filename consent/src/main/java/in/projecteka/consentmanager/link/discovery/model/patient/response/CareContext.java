package in.projecteka.consentmanager.link.discovery.model.patient.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareContext {
    private String referenceNumber;
    private String display;
}
