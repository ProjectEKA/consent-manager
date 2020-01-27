package in.projecteka.consentmanager.consent.model.request;

import in.projecteka.consentmanager.common.model.CareContext;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class GrantedConsent {
    @NotNull(message = "Care contexts are not specified.")
    private List<CareContext> careContexts;

    @NotNull(message = "HI Types are not specified.")
    private HIType[] hiTypes;

    @Valid
    @NotNull(message = "Permission is not specified.")
    private ConsentPermission permission;

    @Valid
    private HIPReference hip;
}
