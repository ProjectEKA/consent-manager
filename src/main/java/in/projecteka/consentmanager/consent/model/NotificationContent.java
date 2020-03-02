package in.projecteka.consentmanager.consent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationContent {
    private String requester;
    private String consentRequestId;
    private String hiTypes;
    private String deepLinkUrl;
}
