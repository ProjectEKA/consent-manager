package in.projecteka.consentmanager.consent.model;

import in.projecteka.consentmanager.consent.model.request.ConsentArtefactReference;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ConsentStatusDetail {
    String id;
    ConsentStatus status;
    List<ConsentArtefactReference> consentArtefacts;
}
