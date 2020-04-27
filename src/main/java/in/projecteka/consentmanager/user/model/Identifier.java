package in.projecteka.consentmanager.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@AllArgsConstructor
@Builder
@Getter
public class Identifier {
    @Valid
    @NotNull(message = "Invalid identifier type")
    IdentifierType type;
    String value;
}
