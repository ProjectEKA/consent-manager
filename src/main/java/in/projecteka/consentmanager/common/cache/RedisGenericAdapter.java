package in.projecteka.consentmanager.common.cache;

import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import reactor.core.publisher.Mono;

import static java.time.Duration.ofMinutes;

@AllArgsConstructor
public class RedisGenericAdapter<T> implements CacheAdapter<String, T> {
    private final ReactiveRedisOperations<String, T> redisOperations;
    private final int expirationInMinutes;

    @Override
    public Mono<T> get(String key) {
        return redisOperations.opsForValue().get(key);
    }

    @Override
    public Mono<Void> put(String key, T value) {
        return redisOperations.opsForValue().set(key, value, ofMinutes(expirationInMinutes)).then();
    }

    @Override
    public Mono<T> getIfPresent(String key) {
        return get(key);
    }

    @Override
    public Mono<Void> invalidate(String key) {
        return redisOperations.expire(key, ofMinutes(0)).then();
    }

    @Override
    public Mono<Boolean> exists(String key) {
        return redisOperations.hasKey(key);
    }

    @Override
    public Mono<Long> increment(String key) {
        return redisOperations.opsForValue().increment(key);
    }
}
