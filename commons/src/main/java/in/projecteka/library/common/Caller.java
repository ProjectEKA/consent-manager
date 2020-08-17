package in.projecteka.library.common;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Builder
@Getter
@EqualsAndHashCode
public class Caller {
    public Caller(String username, Boolean isServiceAccount, String sessionId) {
        this.username = username;
        this.isServiceAccount = isServiceAccount;
        this.sessionId = sessionId;
    }

    public Caller(String username, Boolean isServiceAccount) {
        this.username = username;
        this.isServiceAccount = isServiceAccount;
    }

    private final String username;
    private final Boolean isServiceAccount;
    private String sessionId;
}
