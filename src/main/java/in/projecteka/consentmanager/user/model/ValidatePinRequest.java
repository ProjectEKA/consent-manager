package in.projecteka.consentmanager.user.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import static in.projecteka.consentmanager.common.Constants.SCOPE_CONSENT_APPROVE;
import static in.projecteka.consentmanager.common.Constants.SCOPE_CONSENT_REVOKE;

@AllArgsConstructor
@Getter
public class ValidatePinRequest {
    private final String pin;
    @Valid
    @NotBlank
    @NotNull
    @Pattern(regexp = SCOPE_CONSENT_APPROVE + "|" + SCOPE_CONSENT_REVOKE, message = "Invalid scope provided")
    private final String scope;
}
