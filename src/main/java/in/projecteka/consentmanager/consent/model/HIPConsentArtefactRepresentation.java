package in.projecteka.consentmanager.consent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class HIPConsentArtefactRepresentation {
    private HIPConsentArtefact consentDetail;
    private ConsentStatus status;
    private String signature;
}
