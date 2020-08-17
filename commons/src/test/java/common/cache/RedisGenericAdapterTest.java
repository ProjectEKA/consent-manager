package common.cache;

import in.projecteka.library.common.cache.RedisGenericAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static common.TestBuilders.aLong;
import static common.TestBuilders.localDateTime;
import static common.TestBuilders.string;
import static java.time.Duration.ofMinutes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static reactor.core.publisher.Mono.defer;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.just;
import static reactor.test.StepVerifier.create;

class RedisGenericAdapterTest {

    public static final int EXPIRATION_IN_MINUTES = 5;
    @Mock
    ReactiveRedisOperations<String, LocalDateTime> redisOperations;

    @Mock
    ReactiveValueOperations<String, LocalDateTime> valueOperations;

    private RedisGenericAdapter<LocalDateTime> redisGenericAdapter;

    private RedisGenericAdapter<LocalDateTime> retryableAdapter;

    private static final int RETRY = 3;

    @BeforeEach
    public void init() {
        initMocks(this);
        redisGenericAdapter = new RedisGenericAdapter<>(redisOperations, EXPIRATION_IN_MINUTES, 0);
        retryableAdapter = new RedisGenericAdapter<>(redisOperations, EXPIRATION_IN_MINUTES, RETRY);
    }

    @Test
    void get() {
        var key = string();
        var value = localDateTime();
        when(redisOperations.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(just(value));

        create(redisGenericAdapter.get(key))
                .assertNext(actualValue -> assertThat(actualValue).isEqualTo(value))
                .verifyComplete();
    }

    @Test
    void put() {
        var key = string();
        var value = localDateTime();
        var expiration = ofMinutes(EXPIRATION_IN_MINUTES);
        when(redisOperations.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.set(key, value, expiration)).thenReturn(just(true));

        create(redisGenericAdapter.put(key, value)).verifyComplete();
    }

    @Test
    void getIfPresent() {
        var key = string();
        when(redisOperations.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(empty());

        create(redisGenericAdapter.getIfPresent(key))
                .verifyComplete();
    }

    @Test
    void invalidate() {
        String key = string();
        when(redisOperations.expire(key, ofMinutes(0))).thenReturn(just(true));

        create(redisGenericAdapter.invalidate(key)).verifyComplete();
    }

    @Test
    void exists() {
        var key = string();
        when(redisOperations.hasKey(key)).thenReturn(just(true));

        create(redisGenericAdapter.exists(key))
                .assertNext(exist -> assertThat(exist).isTrue())
                .verifyComplete();
    }

    @Test
    void increment() {
        var key = string();
        var expectedIncrement = aLong();
        when(redisOperations.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(key)).thenReturn(just(expectedIncrement));

        create(redisGenericAdapter.increment(key))
                .assertNext(increment -> assertThat(increment).isEqualTo(expectedIncrement))
                .verifyComplete();
    }

    @Test
    void shouldGiveErrorIfErrorWhileFetching() {
        var key = string();
        var value = LocalDateTime.now();
        when(redisOperations.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenAnswer(new Answer<Mono<LocalDateTime>>() {
            private int numberOfTimesCalled = 0;

            @Override
            public Mono<LocalDateTime> answer(InvocationOnMock invocation) {
                return defer(() -> {
                    if (numberOfTimesCalled++ == RETRY) {
                        return just(value);
                    }
                    return error(new Exception("Failed to get"));
                });
            }
        });

        create(redisGenericAdapter.get(key))
                .expectErrorMessage("Failed to get")
                .verify();
    }


    @Test
    void shouldRetryIfErrorWhileFetchingWhenConfigured() {
        var key = string();
        var value = LocalDateTime.now();
        when(redisOperations.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenAnswer(new Answer<Mono<LocalDateTime>>() {
            private int numberOfTimesCalled = 0;

            @Override
            public Mono<LocalDateTime> answer(InvocationOnMock invocation) {
                return defer(() -> {
                    if (numberOfTimesCalled++ == RETRY) {
                        return just(value);
                    }
                    return error(new Exception("Connection error"));
                });
            }
        });

        create(retryableAdapter.get(key))
                .assertNext(actualValue -> assertThat(actualValue).isEqualTo(value))
                .verifyComplete();
    }
}