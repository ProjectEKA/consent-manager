package common;

import in.projecteka.consentmanager.common.CacheHealth;
import in.projecteka.consentmanager.common.heartbeat.CacheMethodProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;

import static in.projecteka.consentmanager.common.Constants.GUAVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class CacheHealthTest {

    @Mock
    ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;

    @Mock
    ReactiveRedisConnection reactiveRedisConnection;

    @BeforeEach
    void init() {
        initMocks(this);
    }

    @Test
    void isUpWhenRedisIsConnected() {
        var redisEnabled = CacheMethodProperty.builder().methodName("redis").build();
        when(reactiveRedisConnectionFactory.getReactiveConnection()).thenReturn(reactiveRedisConnection);
        CacheHealth cacheHealth = new CacheHealth(redisEnabled, reactiveRedisConnectionFactory);

        boolean up = cacheHealth.isUp();

        assertThat(up).isTrue();
    }

    @Test
    void isUpForGuavaAlways() {
        var redisDisabled = CacheMethodProperty.builder().methodName(GUAVA).build();
        CacheHealth cacheHealth = new CacheHealth(redisDisabled, null);

        boolean up = cacheHealth.isUp();

        assertThat(up).isTrue();
    }
}