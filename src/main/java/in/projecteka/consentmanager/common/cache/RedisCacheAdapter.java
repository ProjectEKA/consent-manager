package in.projecteka.consentmanager.common.cache;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDateTime;

import static java.time.Duration.ofMinutes;
import static reactor.core.publisher.Mono.defer;

@AllArgsConstructor
public class RedisCacheAdapter implements CacheAdapter<String, String> {
    private static final Logger logger = LoggerFactory.getLogger(RedisGenericAdapter.class);
    public static final String RETRIED_AT = "retried at {}";
    private final ReactiveRedisOperations<String, String> stringOps;
    private final int expirationInMinutes;
    private final int retry;

    @Override
    public Mono<String> get(String key) {
        return retryable(stringOps.opsForValue().get(key));
    }

    @Override
    public Mono<Void> put(String key, String value) {
        return retryable(stringOps.opsForValue().set(key, value, ofMinutes(expirationInMinutes)).then());
    }

    @Override
    public Mono<String> getIfPresent(String key) {
        return retryable(get(key));
    }

    @Override
    public Mono<Void> invalidate(String key) {
        return retryable(stringOps.expire(key, ofMinutes(0)).then());
    }

    @Override
    public Mono<Boolean> exists(String key) {
        return retryable(stringOps.hasKey(key));
    }

    @Override
    public Mono<Long> increment(String key) {
        return retryable(stringOps.opsForValue().increment(key));
    }

    private <U> Mono<U> retryable(Mono<U> producer) {
        return defer(() -> producer)
                .doOnError(error -> logger.error(error.getMessage(), error))
                .retryWhen(Retry
                        .backoff(retry, Duration.ofMillis(100)).jitter(0d)
                        .doAfterRetry(rs -> logger.error(RETRIED_AT, LocalDateTime.now()))
                        .onRetryExhaustedThrow((spec, rs) -> rs.failure()));
    }

}
