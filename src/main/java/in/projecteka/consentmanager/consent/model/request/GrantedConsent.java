package in.projecteka.consentmanager.consent.model.request;

import in.projecteka.consentmanager.consent.model.ConsentPermission;
import in.projecteka.consentmanager.consent.model.HIPReference;
import in.projecteka.consentmanager.consent.model.HIType;
import in.projecteka.consentmanager.consent.model.GrantedContext;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class GrantedConsent {
    @Valid
    @NotNull(message = "Care contexts are not specified.")
    private List<GrantedContext> careContexts;

    @NotNull(message = "HI Types are not specified.")
    private HIType[] hiTypes;

    @Valid
    @NotNull(message = "Permission is not specified.")
    private ConsentPermission permission;

    @Valid
    private HIPReference hip;
}
