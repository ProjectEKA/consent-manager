package in.projecteka.consentmanager.consent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class HIPConsentArtefactRepresentation {
    private HIPConsentArtefact consentDetail;
    private ConsentStatus status;
    private String signature;
}
