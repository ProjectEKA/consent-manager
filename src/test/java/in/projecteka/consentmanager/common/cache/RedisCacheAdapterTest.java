package in.projecteka.consentmanager.common.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveValueOperations;

import static in.projecteka.consentmanager.common.TestBuilders.aLong;
import static in.projecteka.consentmanager.common.TestBuilders.string;
import static java.time.Duration.ofMinutes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.just;
import static reactor.test.StepVerifier.create;

class RedisCacheAdapterTest {

    public static final int EXPIRATION_IN_MINUTES = 5;
    @Mock
    ReactiveRedisOperations<String, String> redisOperations;

    @Mock
    ReactiveValueOperations<String, String> valueOperations;

    private RedisCacheAdapter redisCacheAdapter;

    @BeforeEach
    public void init() {
        initMocks(this);
        redisCacheAdapter = new RedisCacheAdapter(redisOperations, EXPIRATION_IN_MINUTES);
    }

    @Test
    void get() {
        var key = string();
        var value = string();
        when(redisOperations.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(just(value));

        create(redisCacheAdapter.get(key))
                .assertNext(actualValue -> assertThat(actualValue).isEqualTo(value))
                .verifyComplete();
    }

    @Test
    void put() {
        var key = string();
        var value = string();
        var expiration = ofMinutes(EXPIRATION_IN_MINUTES);
        when(redisOperations.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.set(key, value, expiration)).thenReturn(just(true));

        create(redisCacheAdapter.put(key, value)).verifyComplete();
    }

    @Test
    void invalidate() {
        var key = string();
        when(redisOperations.expire(key, ofMinutes(0))).thenReturn(just(true));

        create(redisCacheAdapter.invalidate(key)).verifyComplete();
    }

    @Test
    void getIfPresent() {
        var key = string();
        when(redisOperations.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(empty());

        create(redisCacheAdapter.getIfPresent(key))
                .verifyComplete();
    }

    @Test
    void exists() {
        var key = string();
        when(redisOperations.hasKey(key)).thenReturn(just(true));

        create(redisCacheAdapter.exists(key))
                .assertNext(exist -> assertThat(exist).isTrue())
                .verifyComplete();
    }

    @Test
    void increment() {
        var key = string();
        var expectedIncrement = aLong();
        when(redisOperations.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(key)).thenReturn(just(expectedIncrement));

        create(redisCacheAdapter.increment(key))
                .assertNext(increment -> assertThat(increment).isEqualTo(expectedIncrement))
                .verifyComplete();
    }
}