package in.projecteka.consentmanager.consent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import static in.projecteka.consentmanager.common.Constants.API_VERSION;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConsentArtefact implements Serializable {
    @Builder.Default
    private String schemaVersion = API_VERSION;
    private String consentId;
    private LocalDateTime createdAt;
    private ConsentPurpose purpose;
    private PatientReference patient;
    private CMReference consentManager;
    private HIPReference hip;
    private HIUReference hiu;
    private Requester requester;
    private HIType[] hiTypes;
    private ConsentPermission permission;
    private List<GrantedContext> careContexts;
    private Date lastUpdated;
}
