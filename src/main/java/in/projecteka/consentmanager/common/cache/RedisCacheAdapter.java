package in.projecteka.consentmanager.common.cache;

import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import reactor.core.publisher.Mono;

import static java.time.Duration.ofMinutes;

@AllArgsConstructor
public class RedisCacheAdapter implements CacheAdapter<String, String> {
    private final ReactiveRedisOperations<String, String> stringOps;
    private final int expirationInMinutes;

    @Override
    public Mono<String> get(String key) {
        return stringOps.opsForValue().get(key);
    }

    @Override
    public Mono<Void> put(String key, String value) {
        return stringOps.opsForValue().set(key, value, ofMinutes(expirationInMinutes)).then();
    }

    @Override
    public Mono<String> getIfPresent(String key) {
        return get(key);
    }

    @Override
    public Mono<Void> invalidate(String key) {
        return stringOps.delete(key).then();
    }

    @Override
    public Mono<Boolean> exists(String key) {
        return stringOps.hasKey(key);
    }

    @Override
    public Mono<Long> increment(String key) {
        return stringOps.opsForValue().increment(key);
    }
}
