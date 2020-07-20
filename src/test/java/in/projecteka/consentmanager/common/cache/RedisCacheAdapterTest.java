package in.projecteka.consentmanager.common.cache;

class RedisCacheAdapterTest {

    private RedisCacheAdapter redisCacheAdapter;

//    @BeforeEach
//    public void init() {
//        MockitoAnnotations.initMocks(this);
//        redisCacheAdapter = new RedisCacheAdapter(redisClient,5);
//    }
//
//    @Test
//    public void shouldGetFromRedisCache() {
//        String testKey = "foo";
//        String testValue = "bar";
//        when(redisReactiveCommands.get(testKey)).thenReturn(Mono.just(testValue));
//
//        String cachedValue = redisCacheAdapter.get(testKey).block();
//        Assert.assertEquals(testValue,cachedValue);
//        verify(statefulConnection).reactive();
//        verify(redisReactiveCommands).get(testKey);
//    }
//
//    @Test
//    public void shouldSetValueOnRedisCache() {
//        String testKey = "foo";
//        String testValue = "bar";
//        long expiration = 5 * 60L;
//        when(redisReactiveCommands.set(testKey,testValue)).thenReturn(Mono.just("OK"));
//        when(redisReactiveCommands.expire(testKey, expiration)).thenReturn(Mono.just(true));
//
//        StepVerifier.create(redisCacheAdapter.put(testKey, testValue)).verifyComplete();
//        verify(redisReactiveCommands).set(testKey,testValue);
//        verify(redisReactiveCommands).expire(testKey,expiration);
//    }
//
//    @Test
//    public void shouldInvalidate() {
//        String testKey = "foo";
//        when(redisReactiveCommands.expire(testKey, 0L)).thenReturn(Mono.just(true));
//
//        StepVerifier.create(redisCacheAdapter.invalidate(testKey)).verifyComplete();
//        verify(redisReactiveCommands).expire(testKey,0L);
//    }
//
//    @Test
//    public void shouldIncrement() {
//        String testKey = "testKey";
//        long expectedIncrement = 2L;
//        when(redisReactiveCommands.incr(testKey)).thenReturn(Mono.just(expectedIncrement));
//
//        StepVerifier.create(redisCacheAdapter.increment(testKey))
//                .assertNext(increment -> Assertions.assertThat(increment).isEqualTo(expectedIncrement))
//                .verifyComplete();
//
//        verify(redisReactiveCommands).incr(testKey);
//    }
//
//    @Test
//    public void shouldIncrementAndSetExpiry() {
//        String testKey = "testKey";
//        long expectedIncrement = 1L;
//        when(redisReactiveCommands.incr(testKey)).thenReturn(Mono.just(expectedIncrement));
//        when(redisReactiveCommands.expire(testKey,5 * 60)).thenReturn(Mono.just(true));
//
//        StepVerifier.create(redisCacheAdapter.increment(testKey))
//                .assertNext(increment -> Assertions.assertThat(increment).isEqualTo(expectedIncrement))
//                .verifyComplete();
//
//        verify(redisReactiveCommands).incr(testKey);
//        verify(redisReactiveCommands).expire(testKey,5 * 60);
//    }
}