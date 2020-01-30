package in.projecteka.consentmanager.consent.model.response;

import in.projecteka.consentmanager.consent.model.ConsentArtefact;
import in.projecteka.consentmanager.consent.model.ConsentStatus;
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
public class ConsentArtefactRepresentation {
   private ConsentStatus status;
   private ConsentArtefact consentDetail;
   private String signature;
}
