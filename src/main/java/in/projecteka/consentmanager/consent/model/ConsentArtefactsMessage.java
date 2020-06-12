package in.projecteka.consentmanager.consent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentArtefactsMessage {
    private ConsentStatus status;
    private LocalDateTime timestamp;
    private String consentRequestId;
    private List<HIPConsentArtefactRepresentation> consentArtefacts;
    private String hiuId;
}