package in.projecteka.consentmanager.consent.model.response;

import in.projecteka.consentmanager.consent.model.ConsentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConsentArtefactLightRepresentation {
    private ConsentStatus status;
    private ConsentArtefactLight consentDetail;
    private String signature;
}
