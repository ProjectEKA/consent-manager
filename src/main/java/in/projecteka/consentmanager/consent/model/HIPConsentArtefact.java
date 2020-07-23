package in.projecteka.consentmanager.consent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;


@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HIPConsentArtefact implements Serializable {
    private String consentId;
    private LocalDateTime createdAt;
    private ConsentPurpose purpose;
    private PatientReference patient;
    private CMReference consentManager;
    private HIPReference hip;
    private HIType[] hiTypes;
    private ConsentPermission permission;
    private List<GrantedContext> careContexts;
}
