package in.projecteka.consentmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.consentmanager.DestinationsConfig.DestinationInfo;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.link.ClientErrorExceptionHandler;
import in.projecteka.consentmanager.properties.CacheMethodProperty;
import in.projecteka.consentmanager.properties.ClientRegistryProperties;
import in.projecteka.consentmanager.properties.DbOptions;
import in.projecteka.consentmanager.properties.GatewayServiceProperties;
import in.projecteka.consentmanager.properties.IdentityServiceProperties;
import in.projecteka.consentmanager.properties.KeyPairConfig;
import in.projecteka.consentmanager.properties.OtpServiceProperties;
import in.projecteka.consentmanager.properties.RabbitmqOptions;
import in.projecteka.consentmanager.properties.RedisOptions;
import in.projecteka.consentmanager.user.LockedUsersRepository;
import in.projecteka.consentmanager.user.TokenService;
import in.projecteka.consentmanager.user.UserRepository;
import in.projecteka.consentmanager.user.UserServiceProperties;
import in.projecteka.library.clients.ClientRegistryClient;
import in.projecteka.library.clients.IdentityServiceClient;
import in.projecteka.library.clients.OtpServiceClient;
import in.projecteka.library.clients.ServiceAuthenticationClient;
import in.projecteka.library.common.CacheHealth;
import in.projecteka.library.common.CentralRegistry;
import in.projecteka.library.common.GatewayTokenVerifier;
import in.projecteka.library.common.IdentityService;
import in.projecteka.library.common.RequestValidator;
import in.projecteka.library.common.ServiceAuthentication;
import in.projecteka.library.common.ServiceCredential;
import in.projecteka.library.common.cache.CacheAdapter;
import in.projecteka.library.common.cache.LoadingCacheAdapter;
import in.projecteka.library.common.cache.LoadingCacheGenericAdapter;
import in.projecteka.library.common.cache.RedisCacheAdapter;
import in.projecteka.library.common.cache.RedisGenericAdapter;
import in.projecteka.library.common.heartbeat.Heartbeat;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
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
import org.springframework.data.redis.serializer.RedisSerializationContext.RedisSerializationContextBuilder;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.io.IOException;
import java.net.URL;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static in.projecteka.consentmanager.Constants.CONSENT_REQUEST_QUEUE;
import static in.projecteka.consentmanager.Constants.DEFAULT_CACHE_VALUE;
import static in.projecteka.consentmanager.Constants.EXCHANGE;
import static in.projecteka.consentmanager.Constants.HIP_CONSENT_NOTIFICATION_QUEUE;
import static in.projecteka.consentmanager.Constants.HIP_DATA_FLOW_REQUEST_QUEUE;
import static in.projecteka.consentmanager.Constants.HIU_CONSENT_NOTIFICATION_QUEUE;

@Configuration
public class ConsentManagerConfiguration {

    @ConditionalOnProperty(value = "consentmanager.cacheMethod", havingValue = "guava", matchIfMissing = true)
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

    public LoadingCache<String, LocalDateTime> stringLocalDateTimeLoadingCache(int duration) {
        return CacheBuilder
                .newBuilder()
                .expireAfterWrite(duration, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    public LocalDateTime load(String key) {
                        return DEFAULT_CACHE_VALUE;
                    }
                });
    }

    @ConditionalOnProperty(value = "consentmanager.cacheMethod", havingValue = "redis")
    @Bean({"accessToken"})
    public CacheAdapter<String, String> createRedisCacheAdapter(
            ReactiveRedisOperations<String, String> stringReactiveRedisOperations,
            RedisOptions redisOptions) {
        return new RedisCacheAdapter(stringReactiveRedisOperations, 5,
                redisOptions.getRetry());
    }

    @Bean
    public CentralRegistry centralRegistry(ClientRegistryClient clientRegistryClient) {
        return new CentralRegistry(clientRegistryClient);
    }

    @Bean
    public ServiceAuthenticationClient serviceAuthenticationClient(
            @Qualifier("customBuilder") WebClient.Builder webClientBuilder,
            GatewayServiceProperties gatewayServiceProperties) {
        return new ServiceAuthenticationClient(webClientBuilder, gatewayServiceProperties.getBaseUrl());
    }

    @Bean
    public ServiceAuthentication serviceAuthentication(
            ServiceAuthenticationClient serviceAuthenticationClient,
            GatewayServiceProperties gatewayServiceProperties,
            @Qualifier("accessToken") CacheAdapter<String, String> accessToken) {
        return new ServiceAuthentication(serviceAuthenticationClient,
                new ServiceCredential(gatewayServiceProperties.getClientId(),
                        gatewayServiceProperties.getClientSecret()),
                accessToken);
    }

    @Bean
    public ClientRegistryClient clientRegistryClient(@Qualifier("customBuilder") WebClient.Builder builder,
                                                     ClientRegistryProperties clientRegistryProperties) {
        return new ClientRegistryClient(builder, clientRegistryProperties.getUrl());
    }

    @Bean
    public LockedUsersRepository lockedUsersRepository(@Qualifier("readWriteClient") PgPool pgPool) {
        return new LockedUsersRepository(pgPool);
    }

    @Bean("readWriteClient")
    public PgPool readWriteClient(DbOptions dbOptions) {
        PgConnectOptions connectOptions = new PgConnectOptions()
                .setPort(dbOptions.getPort())
                .setHost(dbOptions.getHost())
                .setDatabase(dbOptions.getSchema())
                .setUser(dbOptions.getUser())
                .setPassword(dbOptions.getPassword());

        PoolOptions poolOptions = new PoolOptions().setMaxSize(dbOptions.getPoolSize());

        return PgPool.pool(connectOptions, poolOptions);
    }

    @Bean("readOnlyClient")
    public PgPool readOnlyClient(DbOptions dbOptions) {
        PgConnectOptions connectOptions = new PgConnectOptions()
                .setPort(dbOptions.getReplica().getPort())
                .setHost(dbOptions.getReplica().getHost())
                .setDatabase(dbOptions.getSchema())
                .setUser(dbOptions.getReplica().getUser())
                .setPassword(dbOptions.getReplica().getPassword());

        PoolOptions poolOptions = new PoolOptions().setMaxSize(dbOptions.getReplica().getPoolSize());

        return PgPool.pool(connectOptions, poolOptions);
    }

    @Bean
    // This exception handler needs to be given highest priority compared to DefaultErrorWebExceptionHandler, hence order = -2.
    @Order(-2)
    public ClientErrorExceptionHandler clientErrorExceptionHandler(ErrorAttributes errorAttributes,
                                                                   ResourceProperties resourceProperties,
                                                                   ApplicationContext applicationContext,
                                                                   ServerCodecConfigurer serverCodecConfigurer) {

        ClientErrorExceptionHandler clientErrorExceptionHandler = new ClientErrorExceptionHandler(errorAttributes,
                resourceProperties, applicationContext);
        clientErrorExceptionHandler.setMessageWriters(serverCodecConfigurer.getWriters());
        return clientErrorExceptionHandler;
    }

    @Bean
    public DestinationsConfig destinationsConfig() {
        HashMap<String, DestinationInfo> queues = new HashMap<>();
        queues.put(CONSENT_REQUEST_QUEUE,
                new DestinationInfo(EXCHANGE, CONSENT_REQUEST_QUEUE));
        queues.put(HIU_CONSENT_NOTIFICATION_QUEUE,
                new DestinationInfo(EXCHANGE, HIU_CONSENT_NOTIFICATION_QUEUE));
        queues.put(HIP_CONSENT_NOTIFICATION_QUEUE,
                new DestinationInfo(EXCHANGE, HIP_CONSENT_NOTIFICATION_QUEUE));
        queues.put(HIP_DATA_FLOW_REQUEST_QUEUE,
                new DestinationInfo(EXCHANGE, HIP_DATA_FLOW_REQUEST_QUEUE));
        return new DestinationsConfig(queues);
    }

    @Bean
    public IdentityService identityService(IdentityServiceClient identityServiceClient,
                                           IdentityServiceProperties identityServiceProperties,
                                           @Qualifier("accessToken") CacheAdapter<String, String> accessToken) {
        return new IdentityService(identityServiceClient,
                new ServiceCredential(identityServiceProperties.getClientId(), identityServiceProperties.getClientSecret()),
                accessToken);
    }

    @Bean
    public TokenService tokenService(IdentityServiceProperties identityServiceProperties,
                                     IdentityServiceClient identityServiceClient, UserRepository userRepository) {
        return new TokenService(identityServiceProperties, identityServiceClient, userRepository);
    }

    @SneakyThrows
    @Bean("pinSigning")
    public KeyPair keyPair(KeyPairConfig keyPairConfig) {
        return keyPairConfig.createPinVerificationKeyPair();
    }

    @Bean("keySigningPublicKey")
    public PublicKey publicKey(@Qualifier("pinSigning") KeyPair keyPair) {
        return keyPair.getPublic();
    }

    @Bean("keySigningPrivateKey")
    public PrivateKey privateKey(@Qualifier("pinSigning") KeyPair keyPair) {
        return keyPair.getPrivate();
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

    @Bean
    public Heartbeat heartbeat(IdentityServiceProperties identityServiceProperties,
                               DbOptions dbOptions,
                               RabbitmqOptions rabbitmqOptions,
                               CacheHealth cacheHealth) {
        return new Heartbeat(identityServiceProperties.getBaseUrl(),
                dbOptions.toHeartBeat(),
                rabbitmqOptions.toHeartBeat(),
                cacheHealth);
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
    public UserServiceClient userServiceClient(
            @Qualifier("customBuilder") WebClient.Builder builder,
            UserServiceProperties userServiceProperties,
            IdentityService identityService,
            GatewayServiceProperties gatewayServiceProperties,
            ServiceAuthentication serviceAuthentication,
            @Value("${consentmanager.authorization.header}") String authorizationHeader) {
        return new UserServiceClient(builder.build(),
                userServiceProperties.getUrl(),
                identityService::authenticate,
                gatewayServiceProperties,
                serviceAuthentication,
                authorizationHeader);
    }

    @Bean
    public OtpServiceClient otpServiceClient(@Qualifier("customBuilder") WebClient.Builder builder,
                                             OtpServiceProperties otpServiceProperties) {
        return new OtpServiceClient(builder, otpServiceProperties.getUrl());
    }

    @ConditionalOnProperty(value = "consentmanager.cacheMethod", havingValue = "redis")
    @Bean
    ReactiveRedisOperations<String, LocalDateTime> redisOperations(
            @Qualifier("Lettuce") ReactiveRedisConnectionFactory factory) {
        Jackson2JsonRedisSerializer<LocalDateTime> serializer = new Jackson2JsonRedisSerializer<>(LocalDateTime.class);
        RedisSerializationContextBuilder<String, LocalDateTime> builder =
                RedisSerializationContext.newSerializationContext(new StringRedisSerializer());
        RedisSerializationContext<String, LocalDateTime> context = builder.value(serializer).build();
        return new ReactiveRedisTemplate<>(factory, context);
    }

    @ConditionalOnProperty(value = "consentmanager.cacheMethod", havingValue = "redis")
    @Bean
    ReactiveRedisOperations<String, String> stringReactiveRedisOperations(
            @Qualifier("Lettuce") ReactiveRedisConnectionFactory factory) {
        Jackson2JsonRedisSerializer<String> serializer = new Jackson2JsonRedisSerializer<>(String.class);
        RedisSerializationContextBuilder<String, String> builder =
                RedisSerializationContext.newSerializationContext(new StringRedisSerializer());
        RedisSerializationContext<String, String> context = builder.value(serializer).build();
        return new ReactiveRedisTemplate<>(factory, context);
    }

    @ConditionalOnProperty(value = "consentmanager.cacheMethod", havingValue = "guava", matchIfMissing = true)
    @Bean({"cacheForReplayAttack"})
    public CacheAdapter<String, LocalDateTime> stringLocalDateTimeCacheAdapter() {
        return new LoadingCacheGenericAdapter<>(stringLocalDateTimeLoadingCache(10), DEFAULT_CACHE_VALUE);
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

    @Bean
    public CacheHealth cacheHealth(@Qualifier("Lettuce") ReactiveRedisConnectionFactory redisConnectionFactory,
                                   CacheMethodProperty cacheMethodProperty) {
        return new CacheHealth(cacheMethodProperty.toHeartBeat(), redisConnectionFactory);
    }
}
