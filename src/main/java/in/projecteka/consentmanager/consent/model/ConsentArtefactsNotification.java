package in.projecteka.consentmanager.consent.model;

import in.projecteka.consentmanager.consent.model.response.Consent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentArtefactsNotification {
    private List<Consent> consents;
    private String callBackUrl;
}