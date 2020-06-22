package in.projecteka.consentmanager.consent.model.request;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import in.projecteka.consentmanager.consent.model.ConsentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConsentNotifier {
    private String consentRequestId;
    private ConsentStatus status;
    private List<ConsentArtefactReference> consentArtefacts;

}
