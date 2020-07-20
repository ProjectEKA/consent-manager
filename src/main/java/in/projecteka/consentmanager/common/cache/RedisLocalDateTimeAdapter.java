package in.projecteka.consentmanager.common.cache;

import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static java.time.Duration.ofMinutes;

@AllArgsConstructor
public class RedisLocalDateTimeAdapter implements CacheAdapter<String, LocalDateTime> {
    private final ReactiveRedisOperations<String, LocalDateTime> localDateTimeOps;
    private final int expirationInMinutes;

    @Override
    public Mono<LocalDateTime> get(String key) {
        return localDateTimeOps.opsForValue().get(key);
    }

    @Override
    public Mono<Void> put(String key, LocalDateTime value) {
        return localDateTimeOps.opsForValue().set(key, value, ofMinutes(expirationInMinutes)).then();
    }

    @Override
    public Mono<LocalDateTime> getIfPresent(String key) {
        return get(key);
    }

    @Override
    public Mono<Void> invalidate(String key) {
        return localDateTimeOps.delete(key).then();
    }

    @Override
    public Mono<Boolean> exists(String key) {
        return localDateTimeOps.hasKey(key);
    }

    @Override
    public Mono<Long> increment(String key) {
        return localDateTimeOps.opsForValue().increment(key);
    }
}
