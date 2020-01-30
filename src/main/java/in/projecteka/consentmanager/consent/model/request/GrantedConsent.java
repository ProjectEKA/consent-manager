package in.projecteka.consentmanager.consent.model.request;

import in.projecteka.consentmanager.consent.model.ConsentPermission;
import in.projecteka.consentmanager.consent.model.HIPReference;
import in.projecteka.consentmanager.consent.model.HIType;
import in.projecteka.consentmanager.consent.model.LinkedContext;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class GrantedConsent {
    @NotNull(message = "Care contexts are not specified.")
    private List<LinkedContext> careContexts;

    @NotNull(message = "HI Types are not specified.")
    private HIType[] hiTypes;

    @Valid
    @NotNull(message = "Permission is not specified.")
    private ConsentPermission permission;

    @Valid
    private HIPReference hip;
}
