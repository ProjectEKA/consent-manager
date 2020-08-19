package in.projecteka.consentmanager.properties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConstructorBinding
@ConfigurationProperties("consentmanager.db")
@Getter
@AllArgsConstructor
public class DbOptions {
    private final String host;
    private final int port;
    private final String schema;
    private final String user;
    private final String password;
    private final int poolSize;

    public in.projecteka.library.common.DbOptions toHeartBeat() {
        return new in.projecteka.library.common.DbOptions(host, port);
    }
}
