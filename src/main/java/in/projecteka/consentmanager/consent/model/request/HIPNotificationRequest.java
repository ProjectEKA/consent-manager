package in.projecteka.consentmanager.consent.model.request;

import in.projecteka.consentmanager.consent.model.HIPConsentArtefactRepresentation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class HIPNotificationRequest {
    private HIPConsentArtefactRepresentation notification;
    private UUID requestId;
    private LocalDateTime timestamp;
}
