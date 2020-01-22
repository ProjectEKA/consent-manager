package in.projecteka.consentmanager.consent.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import in.projecteka.consentmanager.consent.model.request.ConsentPermission;
import in.projecteka.consentmanager.consent.model.request.ConsentPurpose;
import in.projecteka.consentmanager.consent.model.request.HIPReference;
import in.projecteka.consentmanager.consent.model.request.HIType;
import in.projecteka.consentmanager.consent.model.request.HIUReference;
import in.projecteka.consentmanager.consent.model.request.PatientReference;
import in.projecteka.consentmanager.consent.model.request.Requester;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class ConsentRequestDetail {
    @JsonProperty("id")
    private String requestId;
    private ConsentStatus status;
    private Date createdAt;
    private ConsentPurpose purpose;
    private PatientReference patient;
    private HIPReference hip;
    private HIUReference hiu;
    private Requester requester;
    private HIType[] hiTypes;
    private ConsentPermission permission;
}
