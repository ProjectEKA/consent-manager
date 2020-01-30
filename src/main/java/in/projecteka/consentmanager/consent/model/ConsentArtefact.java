package in.projecteka.consentmanager.consent.model;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Builder
@Data
public class ConsentArtefact {
    private String consentId;
    private Date createdAt;
    private ConsentPurpose purpose;
    private PatientLinkedContext patient;
    private HIPReference hip;
    private HIUReference hiu;
    private Requester requester;
    private HIType[] hiTypes;
    private ConsentPermission permission;
}
