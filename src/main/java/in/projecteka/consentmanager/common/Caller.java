package in.projecteka.consentmanager.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@AllArgsConstructor
@RequiredArgsConstructor
@Getter
@Builder
public class Caller {
    private final String username;
    private final Boolean isServiceAccount;
    private String sessionId;
}
