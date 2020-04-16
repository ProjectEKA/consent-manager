package in.projecteka.consentmanager.common.cache;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class RedisCacheAdapterTest {
    @Mock
    private RedisClient redisClient;
    @Mock
    private StatefulRedisConnection<String, String> statefulConnection;
    @Mock
    private RedisReactiveCommands<String,String> redisReactiveCommands;
    @InjectMocks
    private RedisCacheAdapter redisCacheAdapter;

    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);
        when(redisClient.connect()).thenReturn(statefulConnection);
        when(statefulConnection.reactive()).thenReturn(redisReactiveCommands);
        redisCacheAdapter.postConstruct();
    }

    @Test
    public void shouldGetFromRedisCache() {
        String testKey = "foo";
        String testValue = "bar";
        when(redisReactiveCommands.get(testKey)).thenReturn(Mono.just(testValue));

        String cachedValue = redisCacheAdapter.get(testKey).block();
        Assert.assertEquals(testValue,cachedValue);
        verify(statefulConnection).reactive();
        verify(redisReactiveCommands).get(testKey);
    }

    @Test
    public void shouldSetValueOnRedisCache() {
        String testKey = "foo";
        String testValue = "bar";
        long expiration = 5 * 60L;
        when(redisReactiveCommands.set(testKey,testValue)).thenReturn(Mono.just("OK"));
        when(redisReactiveCommands.expire(testKey, expiration)).thenReturn(Mono.just(true));

        StepVerifier.create(redisCacheAdapter.put(testKey, testValue)).verifyComplete();
        verify(redisReactiveCommands).set(testKey,testValue);
        verify(redisReactiveCommands).expire(testKey,expiration);
    }

    @Test
    public void shouldInvalidate() {
        String testKey = "foo";
        when(redisReactiveCommands.expire(testKey, 0L)).thenReturn(Mono.just(true));

        StepVerifier.create(redisCacheAdapter.invalidate(testKey)).verifyComplete();
        verify(redisReactiveCommands).expire(testKey,0L);
    }
}