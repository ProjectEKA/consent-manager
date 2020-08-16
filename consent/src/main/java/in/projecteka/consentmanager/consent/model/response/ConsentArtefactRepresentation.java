package in.projecteka.consentmanager.consent.model.response;

import in.projecteka.consentmanager.consent.model.ConsentArtefact;
import in.projecteka.consentmanager.consent.model.ConsentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ConsentArtefactRepresentation {
    private ConsentStatus status;
    private ConsentArtefact consentDetail;
    private String signature;
}
