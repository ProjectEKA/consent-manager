package in.projecteka.user.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.UUID;

import static in.projecteka.library.common.Constants.SCOPE_CHANGE_PIN;
import static in.projecteka.library.common.Constants.SCOPE_CONSENT_APPROVE;
import static in.projecteka.library.common.Constants.SCOPE_CONSENT_REVOKE;

@AllArgsConstructor
@Getter
public class ValidatePinRequest {
    private final UUID requestId;
    private final String pin;
    @Valid
    @NotBlank
    @NotNull
    @Pattern(regexp = SCOPE_CONSENT_APPROVE + "|" + SCOPE_CONSENT_REVOKE + "|" + SCOPE_CHANGE_PIN, message = "Invalid scope provided")
    private final String scope;
}
