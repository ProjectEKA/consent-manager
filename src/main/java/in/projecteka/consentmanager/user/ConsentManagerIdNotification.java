package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.consent.model.Action;
import in.projecteka.consentmanager.consent.model.Communication;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConsentManagerIdNotification {
    private String id;
    private Communication communication;
    private ConsentManagerIdContent content;
    private Action action;
}
