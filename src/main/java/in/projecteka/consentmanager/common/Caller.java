package in.projecteka.consentmanager.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Caller {
    private final String username;
    private final Boolean isServiceAccount;
}
