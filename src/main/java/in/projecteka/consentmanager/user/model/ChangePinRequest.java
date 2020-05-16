package in.projecteka.consentmanager.user.model;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@AllArgsConstructor
@Getter
public class ChangePinRequest {
    private final UUID requestId;
    private final String pin;
}
