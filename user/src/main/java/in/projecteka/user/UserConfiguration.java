package in.projecteka.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.library.clients.IdentityServiceClient;
import in.projecteka.library.clients.OtpServiceClient;
import in.projecteka.library.clients.ServiceAuthenticationClient;
import in.projecteka.library.common.GatewayTokenVerifier;
import in.projecteka.library.common.RequestValidator;
import in.projecteka.library.common.ServiceAuthentication;
import in.projecteka.library.common.ServiceCredential;
import in.projecteka.library.common.cache.CacheAdapter;
import in.projecteka.library.common.cache.LoadingCacheAdapter;
import in.projecteka.library.common.cache.LoadingCacheGenericAdapter;
import in.projecteka.library.common.cache.RedisCacheAdapter;
import in.projecteka.library.common.cache.RedisGenericAdapter;
import in.projecteka.user.clients.UserServiceClient;
import in.projecteka.user.properties.DbOptions;
import in.projecteka.user.properties.GatewayServiceProperties;
import in.projecteka.user.properties.IdentityServiceProperties;
import in.projecteka.user.properties.JWTProperties;
import in.projecteka.user.properties.KeyPairConfig;
import in.projecteka.user.properties.LockedServiceProperties;
import in.projecteka.user.properties.OtpServiceProperties;
import in.projecteka.user.properties.RedisOptions;
import in.projecteka.user.properties.UserServiceProperties;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.ReactiveRedisClusterConnection;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.io.IOException;
import java.net.URL;
import java.security.PrivateKey;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static in.projecteka.library.common.Constants.DEFAULT_CACHE_VALUE;

@Configuration
public class UserConfiguration {

    @Bean
    public UserService userService(UserRepository userRepository,
                                   OtpServiceProperties otpServiceProperties,
                                   OtpServiceClient otpServiceClient,
                                   SignUpService signupService,
                                   IdentityServiceClient identityServiceClient,
                                   TokenService tokenService,
                                   UserServiceProperties userServiceProperties,
                                   OtpAttemptService otpAttemptService,
                                   LockedUserService lockedUserService,
                                   UserServiceClient userServiceClient) {
        return new UserService(userRepository,
                otpServiceProperties,
                otpServiceClient,
                signupService,
                identityServiceClient,
                tokenService,
                userServiceProperties,
                otpAttemptService,
                lockedUserService,
                userServiceClient);
    }

    @Bean
    public UserRepository userRepository(PgPool pgPool) {
        return new UserRepository(pgPool);
    }

    @Bean
    public IdentityServiceClient keycloakClient(@Qualifier("customBuilder") WebClient.Builder builder,
                                                IdentityServiceProperties identityServiceProperties) {
        return new IdentityServiceClient(builder, identityServiceProperties.getBaseUrl());
    }

    @Bean
    public SignUpService authenticatorService(
            JWTProperties jwtProperties,
            @Qualifier("unverifiedSessions") CacheAdapter<String, String> unverifiedSessions,
            @Qualifier("verifiedSessions") CacheAdapter<String, String> verifiedSessions,
            UserServiceProperties userServiceProperties) {
        return new SignUpService(jwtProperties,
                unverifiedSessions,
                verifiedSessions,
                userServiceProperties.getUserCreationTokenValidity());
    }

    @ConditionalOnProperty(value = "user.cache-method", havingValue = "guava", matchIfMissing = true)
    @Bean({"unverifiedSessions", "verifiedSessions", "blockListedTokens", "usedTokens"})
    public CacheAdapter<String, String> createLoadingCacheAdapter() {
        return new LoadingCacheAdapter(createSessionCache(5));
    }

    @ConditionalOnProperty(value = "user.cache-method", havingValue = "guava", matchIfMissing = true)
    @Bean({"dayCache"})
    public CacheAdapter<String, String> createDayLoadingCacheAdapter() {
        return new LoadingCacheAdapter(createSessionCache(24 * 60));
    }

    public LoadingCache<String, String> createSessionCache(int duration) {
        return CacheBuilder
                .newBuilder()
                .expireAfterWrite(duration, TimeUnit.MINUTES)
                .build(new CacheLoader<String, String>() {
                    public String load(String key) {
                        return "";
                    }
                });
    }

    @ConditionalOnProperty(value = "user.cache-method", havingValue = "redis")
    @Bean({"unverifiedSessions", "verifiedSessions", "blockListedTokens", "usedTokens"})
    public CacheAdapter<String, String> createRedisCacheAdapter(
            ReactiveRedisOperations<String, String> stringReactiveRedisOperations,
            RedisOptions redisOptions) {
        return new RedisCacheAdapter(stringReactiveRedisOperations, 5, redisOptions.getRetry());
    }

    @ConditionalOnProperty(value = "user.cache-method", havingValue = "redis")
    @Bean({"dayCache"})
    public CacheAdapter<String, String> createDayRedisCacheAdapter(
            ReactiveRedisOperations<String, String> stringReactiveRedisOperations,
            RedisOptions redisOptions) {
        return new RedisCacheAdapter(stringReactiveRedisOperations, 24 * 60,
                redisOptions.getRetry());
    }

    @Bean
    LockedUserService lockedUserService(LockedUsersRepository lockedUsersRepository,
                                        LockedServiceProperties lockedServiceProperties) {
        return new LockedUserService(lockedUsersRepository, lockedServiceProperties);
    }

    @Bean
    public SessionService sessionService(
            TokenService tokenService,
            @Qualifier("blockListedTokens") CacheAdapter<String, String> blockListedTokens,
            @Qualifier("unverifiedSessions") CacheAdapter<String, String> unverifiedSessions,
            LockedUserService lockedUserService,
            UserRepository userRepository,
            OtpServiceClient otpServiceClient,
            OtpServiceProperties otpServiceProperties,
            OtpAttemptService otpAttemptService) {
        return new SessionService(tokenService,
                blockListedTokens,
                unverifiedSessions,
                lockedUserService,
                userRepository,
                otpServiceClient,
                otpServiceProperties,
                otpAttemptService);
    }

    @Bean
    public TransactionPinRepository transactionPinRepository(PgPool dbClient) {
        return new TransactionPinRepository(dbClient);
    }

    @Bean
    public BCryptPasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public TransactionPinService transactionPinService(TransactionPinRepository transactionPinRepository,
                                                       BCryptPasswordEncoder encoder,
                                                       @Qualifier("keySigningPrivateKey") PrivateKey privateKey,
                                                       UserServiceProperties userServiceProperties,
                                                       @Qualifier("dayCache") CacheAdapter<String, String> dayCache) {
        return TransactionPinService.builder()
                .encoder(encoder)
                .privateKey(privateKey)
                .userServiceProperties(userServiceProperties)
                .dayCache(dayCache)
                .transactionPinRepository(transactionPinRepository)
                .build();
    }

    @Bean
    public ProfileService profileService(UserService userService, TransactionPinService transactionPinService) {
        return new ProfileService(userService, transactionPinService);
    }

    @Bean
    public PgPool readWriteClient(DbOptions dbOptions) {
        PgConnectOptions connectOptions = new PgConnectOptions()
                .setPort(dbOptions.getPort())
                .setHost(dbOptions.getHost())
                .setDatabase(dbOptions.getSchema())
                .setUser(dbOptions.getUser())
                .setPassword(dbOptions.getPassword());

        PoolOptions poolOptions = new PoolOptions()
                .setMaxSize(dbOptions.getPoolSize());

        return PgPool.pool(connectOptions, poolOptions);
    }

    @Bean
    @ConditionalOnProperty(value = "webclient.keepalive", havingValue = "false")
    public ClientHttpConnector clientHttpConnector() {
        return new ReactorClientHttpConnector(HttpClient.create(ConnectionProvider.newConnection()));
    }

    @Bean("customBuilder")
    public WebClient.Builder webClient(final ClientHttpConnector clientHttpConnector, ObjectMapper objectMapper) {
        return WebClient
                .builder()
                .exchangeStrategies(exchangeStrategies(objectMapper))
                .clientConnector(clientHttpConnector);
    }

    private ExchangeStrategies exchangeStrategies(ObjectMapper objectMapper) {
        var encoder = new Jackson2JsonEncoder(objectMapper);
        var decoder = new Jackson2JsonDecoder(objectMapper);
        return ExchangeStrategies
                .builder()
                .codecs(configurer -> {
                    configurer.defaultCodecs().jackson2JsonEncoder(encoder);
                    configurer.defaultCodecs().jackson2JsonDecoder(decoder);
                }).build();
    }

    @Bean
    public ServiceAuthentication serviceAuthentication(
            ServiceAuthenticationClient serviceAuthenticationClient,
            GatewayServiceProperties gatewayServiceProperties,
            @Qualifier("accessToken") CacheAdapter<String, String> accessTokenCache) {
        return new ServiceAuthentication(serviceAuthenticationClient,
                new ServiceCredential(gatewayServiceProperties.getClientId(), gatewayServiceProperties.getClientSecret()),
                accessTokenCache);
    }

    @Bean
    public ServiceAuthenticationClient serviceAuthenticationClient(
            @Qualifier("customBuilder") WebClient.Builder builder,
            GatewayServiceProperties gatewayServiceProperties) {
        return new ServiceAuthenticationClient(builder, gatewayServiceProperties.getBaseUrl());
    }

    @Bean
    public UserServiceClient userServiceClient(ServiceAuthentication serviceAuthentication,
                                               GatewayServiceProperties gatewayServiceProperties,
                                               @Qualifier("customBuilder") WebClient.Builder builder) {
        return new UserServiceClient(builder.build(), gatewayServiceProperties, serviceAuthentication);
    }

    @Bean
    public OtpAttemptRepository otpAttemptRepository(PgPool pgPool) {
        return new OtpAttemptRepository(pgPool);
    }

    @Bean
    public OtpAttemptService otpAttemptService(OtpAttemptRepository otpAttemptRepository,
                                               UserServiceProperties userServiceProperties) {
        return new OtpAttemptService(otpAttemptRepository, userServiceProperties);
    }

    @Bean
    public LockedUsersRepository lockedUsersRepository(PgPool pgPool) {
        return new LockedUsersRepository(pgPool);
    }

    @Bean
    public TokenService tokenService(UserRepository userRepository,
                                     IdentityServiceProperties identityServiceProperties,
                                     IdentityServiceClient identityServiceClient) {
        return new TokenService(identityServiceProperties, identityServiceClient, userRepository);
    }

    @Bean
    public OtpServiceClient otpServiceClient(OtpServiceProperties otpServiceProperties,
                                             @Qualifier("customBuilder") WebClient.Builder builder) {
        return new OtpServiceClient(builder, otpServiceProperties.getUrl());
    }

    @ConditionalOnProperty(value = "user.cache-method", havingValue = "guava", matchIfMissing = true)
    @Bean({"accessToken"})
    public CacheAdapter<String, String> createLoadingCacheAdapterForAccessToken() {
        return new LoadingCacheAdapter(stringStringLoadingCache(5));
    }

    public LoadingCache<String, String> stringStringLoadingCache(int duration) {
        return CacheBuilder
                .newBuilder()
                .expireAfterWrite(duration, TimeUnit.MINUTES)
                .build(new CacheLoader<String, String>() {
                    public String load(String key) {
                        return "";
                    }
                });
    }

    @ConditionalOnProperty(value = "user.cache-method", havingValue = "redis")
    @Bean({"accessToken"})
    public CacheAdapter<String, String> accessTokenCache(
            ReactiveRedisOperations<String, String> stringReactiveRedisOperations,
            RedisOptions redisOptions) {
        return new RedisCacheAdapter(stringReactiveRedisOperations, 5, redisOptions.getRetry());
    }

    @Bean("keySigningPrivateKey")
    public PrivateKey keyPair(KeyPairConfig keyPairConfig) {
        return keyPairConfig.createPinVerificationKeyPair().getPrivate();
    }

    @Bean("centralRegistryJWKSet")
    public JWKSet jwkSet(GatewayServiceProperties gatewayServiceProperties)
            throws IOException, ParseException {
        return JWKSet.load(new URL(gatewayServiceProperties.getJwkUrl()));
    }

    @Bean("identityServiceJWKSet")
    public JWKSet identityServiceJWKSet(IdentityServiceProperties identityServiceProperties)
            throws IOException, ParseException {
        return JWKSet.load(new URL(identityServiceProperties.getJwkUrl()));
    }

    @Bean
    public GatewayTokenVerifier centralRegistryTokenVerifier(@Qualifier("centralRegistryJWKSet") JWKSet jwkSet) {
        return new GatewayTokenVerifier(jwkSet);
    }

    @Bean
    public RequestValidator requestValidator(
            @Qualifier("cacheForReplayAttack") CacheAdapter<String, LocalDateTime> cacheForReplayAttack) {
        return new RequestValidator(cacheForReplayAttack);
    }

    @ConditionalOnProperty(value = "consentmanager.cacheMethod", havingValue = "guava", matchIfMissing = true)
    @Bean({"cacheForReplayAttack"})
    public CacheAdapter<String, LocalDateTime> stringLocalDateTimeCacheAdapter() {
        return new LoadingCacheGenericAdapter<>(stringLocalDateTimeLoadingCache(), DEFAULT_CACHE_VALUE);
    }

    @ConditionalOnProperty(value = "consentmanager.cacheMethod", havingValue = "redis")
    @Bean({"cacheForReplayAttack"})
    public CacheAdapter<String, LocalDateTime> createRedisCacheAdapterForReplayAttack(
            ReactiveRedisOperations<String, LocalDateTime> localDateTimeOps,
            RedisOptions redisOptions) {
        return new RedisGenericAdapter<>(localDateTimeOps, 10, redisOptions.getRetry());
    }

    @ConditionalOnProperty(value = "consentmanager.cacheMethod", havingValue = "redis")
    @Bean("Lettuce")
    ReactiveRedisConnectionFactory redisConnection(RedisOptions redisOptions) {
        var socketOptions = SocketOptions.builder().keepAlive(redisOptions.isKeepAliveEnabled()).build();
        var clientConfiguration = LettuceClientConfiguration.builder()
                .readFrom(redisOptions.getReadFrom())
                .clientOptions(ClientOptions.builder().socketOptions(socketOptions).build())
                .build();
        var configuration = new RedisStandaloneConfiguration(redisOptions.getHost(), redisOptions.getPort());
        configuration.setPassword(redisOptions.getPassword());
        return new LettuceConnectionFactory(configuration, clientConfiguration);
    }

    @ConditionalOnProperty(value = "consentmanager.cacheMethod", havingValue = "guava", matchIfMissing = true)
    @Bean("Lettuce")
    ReactiveRedisConnectionFactory dummyRedisConnection() {
        return new ReactiveRedisConnectionFactory() {
            @Override
            public ReactiveRedisConnection getReactiveConnection() {
                return null;
            }

            @Override
            public ReactiveRedisClusterConnection getReactiveClusterConnection() {
                return null;
            }

            @Override
            public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
                return null;
            }
        };
    }

    private LoadingCache<String, LocalDateTime> stringLocalDateTimeLoadingCache() {
        return CacheBuilder
                .newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    public LocalDateTime load(String key) {
                        return DEFAULT_CACHE_VALUE;
                    }
                });
    }

    @ConditionalOnProperty(value = "consentmanager.cacheMethod", havingValue = "redis")
    @Bean
    ReactiveRedisOperations<String, LocalDateTime> redisOperations(
            @Qualifier("Lettuce") ReactiveRedisConnectionFactory factory) {
        Jackson2JsonRedisSerializer<LocalDateTime> serializer = new Jackson2JsonRedisSerializer<>(LocalDateTime.class);
        RedisSerializationContext.RedisSerializationContextBuilder<String, LocalDateTime> builder =
                RedisSerializationContext.newSerializationContext(new StringRedisSerializer());
        RedisSerializationContext<String, LocalDateTime> context = builder.value(serializer).build();
        return new ReactiveRedisTemplate<>(factory, context);
    }
}
