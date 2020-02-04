package in.projecteka.consentmanager.consent.model;

import in.projecteka.consentmanager.consent.model.request.ConsentArtefactNotificationRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentArtefactsNotificationMessage {
    private ConsentArtefactNotificationRequest consentArtefactNotificationRequest;
    private String callBackUrl;
}