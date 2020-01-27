package in.projecteka.consentmanager.consent.model;

import in.projecteka.consentmanager.consent.model.request.ConsentPermission;
import in.projecteka.consentmanager.consent.model.request.ConsentPurpose;
import in.projecteka.consentmanager.consent.model.request.HIPReference;
import in.projecteka.consentmanager.consent.model.request.HIType;
import in.projecteka.consentmanager.consent.model.request.HIUReference;
import in.projecteka.consentmanager.consent.model.request.PatientReference;
import in.projecteka.consentmanager.consent.model.request.Requester;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Builder
@Data
public class ConsentArtefact {
    private String id;
    private String requestId;
    private Date createdAt;
    private ConsentPurpose purpose;
    private PatientReference patient;
    private HIPReference hip;
    private HIUReference hiu;
    private Requester requester;
    private HIType[] hiTypes;
    private ConsentPermission permission;
}
