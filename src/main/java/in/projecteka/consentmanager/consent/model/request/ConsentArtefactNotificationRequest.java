package in.projecteka.consentmanager.consent.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactReference;
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
public class ConsentArtefactNotificationRequest {
    private String consentRequestId;
    private List<ConsentArtefactReference> consents;
}
