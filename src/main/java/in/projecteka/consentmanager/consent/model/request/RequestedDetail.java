package in.projecteka.consentmanager.consent.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import in.projecteka.consentmanager.consent.model.ConsentPermission;
import in.projecteka.consentmanager.consent.model.ConsentPurpose;
import in.projecteka.consentmanager.consent.model.HIPReference;
import in.projecteka.consentmanager.consent.model.HIType;
import in.projecteka.consentmanager.consent.model.HIUReference;
import in.projecteka.consentmanager.consent.model.PatientReference;
import in.projecteka.consentmanager.consent.model.Requester;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RequestedDetail {
    @Valid
    @NotNull(message = "Purpose is not specified.")
    private ConsentPurpose purpose;

    @Valid
    @NotNull(message = "Patient is not specified.")
    private PatientReference patient;

    @Valid
    private HIPReference hip;

    @Valid
    @NotNull(message = "HIU is not specified.")
    private HIUReference hiu;

    @Valid
    @NotNull(message = "Requester is not specified.")
    private Requester requester;

    @NotNull(message = "HI Types are not specified.")
    private HIType[] hiTypes;

    @Valid
    @NotNull(message = "Permission is not specified.")
    private ConsentPermission permission;

    @NotNull(message = "Call back url not specified.")
    private String callBackUrl;
}
