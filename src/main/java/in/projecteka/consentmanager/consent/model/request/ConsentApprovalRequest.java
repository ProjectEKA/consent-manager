package in.projecteka.consentmanager.consent.model.request;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class ConsentApprovalRequest {
    @Valid
    @NotNull(message = "Consents are not specified")
    private List<GrantedConsent> consents;
    @Valid
    @NotNull(message = "Callback Url not specified")
    private String callBackUrl;
}
