package in.projecteka.library.common.heartbeat;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RabbitmqOptions {
    private final String host;
    private final int port;
    private final String username;
    private final String password;
}
