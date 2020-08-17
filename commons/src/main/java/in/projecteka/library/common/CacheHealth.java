package in.projecteka.library.common;

import in.projecteka.library.common.heartbeat.CacheMethodProperty;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;

import java.util.function.BooleanSupplier;

@AllArgsConstructor
public class CacheHealth {
    private final CacheMethodProperty cacheMethodProperty;
    private final ReactiveRedisConnectionFactory redisConnectionFactory;

    public boolean isUp() {
        BooleanSupplier checkRedis = () -> {
            try (ReactiveRedisConnection ignored = redisConnectionFactory.getReactiveConnection()) {
                return true;
            } catch (Exception e) {
                return false;
            }
        };
        return cacheMethodProperty.getMethodName().equalsIgnoreCase(Constants.GUAVA) || checkRedis.getAsBoolean();
    }
}
