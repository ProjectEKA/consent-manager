package in.projecteka.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConditionalOnProperty(value = "user.cacheMethod", havingValue = "redis")
@ConfigurationProperties(prefix = "user.redis")
@Getter
@AllArgsConstructor
@ConstructorBinding
public class RedisOptions {
    private final String host;
    private final int port;
    private final String password;
    private final boolean keepAliveEnabled;
    private final ReadFrom readFrom;
    private final int retry;

    public io.lettuce.core.ReadFrom getReadFrom() {
        switch (readFrom) {
            case NEAREST:
                return io.lettuce.core.ReadFrom.NEAREST;
            case REPLICA:
                return io.lettuce.core.ReadFrom.REPLICA_PREFERRED;
            case UPSTREAM:
                return io.lettuce.core.ReadFrom.MASTER_PREFERRED;
            default:
                return io.lettuce.core.ReadFrom.ANY;
        }
    }
}
