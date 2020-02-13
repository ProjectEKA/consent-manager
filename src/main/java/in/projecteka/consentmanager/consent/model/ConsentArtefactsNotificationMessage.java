package in.projecteka.consentmanager.consent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentArtefactsNotificationMessage {
    private String requestId;
    private List<HIPConsentArtefactRepresentation> consentArtefacts;
    private String hiuCallBackUrl;
}