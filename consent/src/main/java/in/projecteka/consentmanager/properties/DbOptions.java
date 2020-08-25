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
    private final boolean replicaReadEnabled;
    private final Replica replica;

    public Replica getReplica() {
        return replica != null && replicaReadEnabled
                ? replica
                : new Replica(host, port, user, password, getReadPoolSize());
    }

    private int getReadPoolSize() {
        return poolSize / 2 + poolSize % 2;
    }

    public int getPoolSize() {
        return replica != null && replicaReadEnabled
                ? poolSize
                : poolSize / 2;
    }

    public in.projecteka.library.common.DbOptions toHeartBeat() {
        return new in.projecteka.library.common.DbOptions(host, port);
    }
}

