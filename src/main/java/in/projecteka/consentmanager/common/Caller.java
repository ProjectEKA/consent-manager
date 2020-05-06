package in.projecteka.consentmanager.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@AllArgsConstructor
@RequiredArgsConstructor
@Getter
public class Caller {
    private final String username;
    private final Boolean isServiceAccount;
    private String sessionId;
}
