package in.projecteka.consentmanager.consent.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConsentDetail {
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
}
