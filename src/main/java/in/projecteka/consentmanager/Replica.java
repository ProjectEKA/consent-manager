package in.projecteka.consentmanager;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Replica {
    private final String host;
    private final int port;
    private final String user;
    private final String password;
    private final int poolSize;
}
