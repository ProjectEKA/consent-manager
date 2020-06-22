package in.projecteka.consentmanager.common.cache;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class RedisCacheAdapter implements CacheAdapter<String, String> {

    private final RedisClient redisClient;
    private StatefulRedisConnection<String, String> statefulConnection;
    private int expirationInMinutes;

    public RedisCacheAdapter(RedisClient redisClient, int expirationInMinutes) {
        this.redisClient = redisClient;
        this.expirationInMinutes = expirationInMinutes;
    }

    @PostConstruct
    public void postConstruct() {
        statefulConnection = redisClient.connect();
    }

    @PreDestroy
    public void preDestroy() {
        statefulConnection.close();
        redisClient.shutdown();
    }

    @Override
    public Mono<String> get(String key) {
        RedisReactiveCommands<String, String> redisCommands = statefulConnection.reactive();
        return redisCommands.get(key);
    }

    @Override
    public Mono<Void> put(String key, String value) {
        RedisReactiveCommands<String, String> redisCommands = statefulConnection.reactive();
        return redisCommands.set(key, value)
                .then(redisCommands.expire(key, expirationInMinutes * 60L))
                .then();
    }

    @Override
    public Mono<String> getIfPresent(String key) {
        return get(key);
    }

    @Override
    public Mono<Void> invalidate(String key) {
        RedisReactiveCommands<String, String> redisCommands = statefulConnection.reactive();
        return redisCommands.expire(key, 0L).then();
    }

    @Override
    public Mono<Boolean> exists(String key) {
        RedisReactiveCommands<String, String> redisCommands = statefulConnection.reactive();
        return redisCommands.exists(key).flatMap(existCount -> Mono.just(existCount > 0));
    }

    @Override
    public Mono<Long> increment(String key) {
        RedisReactiveCommands<String, String> redisCommands = statefulConnection.reactive();
        return redisCommands.incr(key)
                .filter(count -> count != 1)
                .switchIfEmpty(Mono.defer(() -> redisCommands.expire(key, expirationInMinutes * 60L).thenReturn(1L)));
    }
}
