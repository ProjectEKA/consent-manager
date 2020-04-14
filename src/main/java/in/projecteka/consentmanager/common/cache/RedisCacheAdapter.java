package in.projecteka.consentmanager.common.cache;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Optional;

public class RedisCacheAdapter implements ICacheAdapter<String, Optional<String>> {

    private final RedisClient redisClient;
    private StatefulRedisConnection<String, String> statefulConnection;

    public RedisCacheAdapter(RedisClient redisClient) {
        this.redisClient = redisClient;
    }

    @PostConstruct
    public void postConstruct() {
        statefulConnection = redisClient.connect();
    }

    @PreDestroy
    public void preDestroy () {
        statefulConnection.close();
        redisClient.shutdown();
    }
    @Override
    public Optional<String> get(String key) {
        RedisCommands<String, String> redisCommands = statefulConnection.sync();
        return Optional.ofNullable(redisCommands.get(key));
    }

    @Override
    public void put(String key, Optional<String> value) {
        RedisCommands<String, String> redisCommands = statefulConnection.sync();
        redisCommands.set(key, value.orElseGet((String::new)));
        redisCommands.expire(key, 5 * 60L);
    }

    @Override
    public Optional<String> getIfPresent(String key) {
        return get(key);
    }

    @Override
    public void invalidate(String key) {
        RedisCommands<String, String> redisCommands = statefulConnection.sync();
        redisCommands.expire(key, 0l);
    }
}
