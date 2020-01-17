package in.projecteka.consentmanager.consent.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class ConsentDetail {
    private ConsentPurpose purpose;
    private PatientReference patient;
    private HIPReference hip;
    private HIUReference hiu;
    private Requester requester;
    private HIType[] hiTypes;
    private ConsentPermission permission;
}
