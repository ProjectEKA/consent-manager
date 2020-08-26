package in.projecteka.dataflow;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.dataflow.DestinationsConfig.DestinationInfo;
import in.projecteka.dataflow.properties.DataFlowConsentManagerProperties;
import in.projecteka.dataflow.properties.DbOptions;
import in.projecteka.dataflow.properties.GatewayServiceProperties;
import in.projecteka.dataflow.properties.IdentityServiceProperties;
import in.projecteka.dataflow.properties.RedisOptions;
import in.projecteka.library.clients.IdentityServiceClient;
import in.projecteka.library.clients.ServiceAuthenticationClient;
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
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static in.projecteka.dataflow.Constants.EXCHANGE;
import static in.projecteka.dataflow.Constants.HIP_DATA_FLOW_REQUEST_QUEUE;
import static in.projecteka.library.common.Constants.DEFAULT_CACHE_VALUE;

@Configuration
public class DataFlowConfiguration {

    @ConditionalOnProperty(value = "dataflow.cache-method", havingValue = "guava", matchIfMissing = true)
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

    @ConditionalOnProperty(value = "dataflow.cache-method", havingValue = "redis")
    @Bean({"accessToken"})
    public CacheAdapter<String, String> accessTokenCache(
            ReactiveRedisOperations<String, String> stringReactiveRedisOperations,
            RedisOptions redisOptions) {
        return new RedisCacheAdapter(stringReactiveRedisOperations, 5, redisOptions.getRetry());
    }

    @ConditionalOnProperty(value = "consentmanager.cacheMethod", havingValue = "guava", matchIfMissing = true)
    @Bean({"cacheForReplayAttack"})
    public CacheAdapter<String, LocalDateTime> stringLocalDateTimeCacheAdapter() {
        return new LoadingCacheGenericAdapter<>(stringLocalDateTimeLoadingCache(10), DEFAULT_CACHE_VALUE);
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
    public DestinationsConfig destinationsConfig() {
        HashMap<String, DestinationInfo> queues = new HashMap<>();
        queues.put(HIP_DATA_FLOW_REQUEST_QUEUE,
                new DestinationInfo(EXCHANGE, HIP_DATA_FLOW_REQUEST_QUEUE));
        return new DestinationsConfig(queues);
    }

    @Bean
    public in.projecteka.dataflow.PostDataFlowRequestApproval postDataFlowRequestApproval(AmqpTemplate amqpTemplate,
                                                                                          DestinationsConfig destinationsConfig) {
        return new in.projecteka.dataflow.PostDataFlowRequestApproval(amqpTemplate, destinationsConfig);
    }

    @Bean
    public RequestValidator requestValidator(
            @Qualifier("cacheForReplayAttack") CacheAdapter<String, LocalDateTime> cacheForReplayAttack) {
        return new RequestValidator(cacheForReplayAttack);
    }

    @Bean
    public Jackson2JsonMessageConverter converter() {
        var objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public MessageListenerContainerFactory messageListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter) {
        return new MessageListenerContainerFactory(connectionFactory, jackson2JsonMessageConverter);
    }

    @Bean
    public DataFlowBroadcastListener dataFlowBroadcastListener(
            @Qualifier("customBuilder") WebClient.Builder builder,
            DataFlowConsentManagerProperties dataFlowConsentManagerProperties,
            IdentityService identityService,
            MessageListenerContainerFactory messageListenerContainerFactory,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter,
            DataRequestNotifier dataRequestNotifier) {
        return new DataFlowBroadcastListener(messageListenerContainerFactory,
                jackson2JsonMessageConverter,
                dataRequestNotifier,
                new ConsentManagerClient(builder,
                        dataFlowConsentManagerProperties.getUrl(),
                        identityService::authenticate));
    }

    @Bean
    public DataFlowRequestClient dataFlowRequestClient(@Qualifier("customBuilder") WebClient.Builder builder,
                                                       GatewayServiceProperties gatewayServiceProperties,
                                                       ServiceAuthentication serviceAuthentication) {
        return new DataFlowRequestClient(builder, gatewayServiceProperties, serviceAuthentication);
    }

    @Bean
    public IdentityServiceClient keycloakClient(@Qualifier("customBuilder") WebClient.Builder builder,
                                                IdentityServiceProperties identityServiceProperties) {
        return new IdentityServiceClient(builder, identityServiceProperties.getBaseUrl());
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
    public DataFlowRequester dataRequest(@Qualifier("customBuilder") WebClient.Builder builder,
                                         DataFlowRequestRepository dataFlowRequestRepository,
                                         PostDataFlowRequestApproval postDataFlowRequestApproval,
                                         DataFlowConsentManagerProperties dataFlowConsentManagerProperties,
                                         IdentityService identityService,
                                         DataFlowRequestClient dataFlowRequestClient) {
        return new DataFlowRequester(
                new ConsentManagerClient(builder,
                        dataFlowConsentManagerProperties.getUrl(),
                        identityService::authenticate),
                dataFlowRequestRepository,
                postDataFlowRequestApproval,
                dataFlowRequestClient);
    }

    @Bean
    public PgPool pgPool(DbOptions dbOptions) {
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
    public DataFlowRequestRepository dataRequestRepository(PgPool pgPool) {
        return new DataFlowRequestRepository(pgPool);
    }

    @Bean
    public ServiceAuthenticationClient serviceAuthenticationClient(
            @Qualifier("customBuilder") WebClient.Builder builder,
            GatewayServiceProperties gatewayServiceProperties) {
        return new ServiceAuthenticationClient(builder, gatewayServiceProperties.getBaseUrl());
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

    @Bean("gatewayJWKSet")
    public JWKSet gatewayJWKSet(GatewayServiceProperties gatewayServiceProperties)
            throws IOException, ParseException {
        return JWKSet.load(new URL(gatewayServiceProperties.getJwkUrl()));
    }

    @Bean
    public GatewayTokenVerifier gatewayTokenVerifier(@Qualifier("gatewayJWKSet") JWKSet jwkSet) {
        return new GatewayTokenVerifier(jwkSet);
    }

    @Bean
    public DataRequestNotifier dataFlowClient(@Qualifier("customBuilder") WebClient.Builder builder,
                                              ServiceAuthentication serviceAuthentication,
                                              GatewayServiceProperties gatewayServiceProperties) {
        return new DataRequestNotifier(builder.build(), serviceAuthentication::authenticate, gatewayServiceProperties);
    }
}
