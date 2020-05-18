package in.projecteka.consentmanager;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.consentmanager.clients.ClientRegistryClient;
import in.projecteka.consentmanager.clients.IdentityServiceClient;
import in.projecteka.consentmanager.clients.properties.ClientRegistryProperties;
import in.projecteka.consentmanager.clients.properties.IdentityServiceProperties;
import in.projecteka.consentmanager.common.CentralRegistry;
import in.projecteka.consentmanager.common.CentralRegistryTokenVerifier;
import in.projecteka.consentmanager.common.IdentityService;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import in.projecteka.consentmanager.common.cache.LoadingCacheAdapter;
import in.projecteka.consentmanager.common.cache.RedisCacheAdapter;
import in.projecteka.consentmanager.common.cache.RedisOptions;
import in.projecteka.consentmanager.link.ClientErrorExceptionHandler;
import in.projecteka.consentmanager.user.LockedUsersRepository;
import in.projecteka.consentmanager.user.TokenService;
import in.projecteka.consentmanager.user.UserRepository;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.net.URL;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.text.ParseException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Configuration
public class ConsentManagerConfiguration {
    public static final String HIU_CONSENT_NOTIFICATION_QUEUE = "hiu-consent-notification-queue";
    public static final String HIP_CONSENT_NOTIFICATION_QUEUE = "hip-consent-notification-queue";
    public static final String HIP_DATA_FLOW_REQUEST_QUEUE = "hip-data-flow-request-queue";
    public static final String CONSENT_REQUEST_QUEUE = "consent-request-queue";
    public static final String DEAD_LETTER_QUEUE = "cm-dead-letter-queue";
    private static final String CM_DEAD_LETTER_EXCHANGE = "cm-dead-letter-exchange";
    private static final String CM_DEAD_LETTER_ROUTING_KEY = "cm-dead-letter";

    @ConditionalOnProperty(value = "consentmanager.cacheMethod", havingValue = "guava", matchIfMissing = true)
    @Bean({"accessToken"})
    public CacheAdapter<String, String> createLoadingCacheAdapterForAccessToken() {
        return new LoadingCacheAdapter(createSessionCache(5));
    }

    @ConditionalOnProperty(value = "consentmanager.cacheMethod", havingValue = "guava", matchIfMissing = true)
    @Bean({"refreshToken"})
    public CacheAdapter<String, String> createLoadingCacheAdapterForRefreshToken() {
        return new LoadingCacheAdapter(createSessionCache(30));
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


    @ConditionalOnProperty(value = "consentmanager.cacheMethod", havingValue = "redis")
    @Bean({"accessToken"})
    public CacheAdapter<String, String> createRedisCacheAdapter(RedisOptions redisOptions) {
        RedisClient redisClient = getRedisClient(redisOptions);
        return new RedisCacheAdapter(redisClient, 5);
    }

    @ConditionalOnProperty(value = "consentmanager.cacheMethod", havingValue = "redis")
    @Bean({"refreshToken"})
    public CacheAdapter<String, String> createDayRedisCacheAdapter(RedisOptions redisOptions) {
        RedisClient redisClient = getRedisClient(redisOptions);
        return new RedisCacheAdapter(redisClient, 30);
    }

    private RedisClient getRedisClient(RedisOptions redisOptions) {
        RedisURI redisUri = RedisURI.Builder.
                redis(redisOptions.getHost())
                .withPort(redisOptions.getPort())
                .withPassword(redisOptions.getPassword())
                .build();
        return RedisClient.create(redisUri);
    }

    @Bean
    public CentralRegistry centralRegistry(ClientRegistryClient clientRegistryClient,
                                           ClientRegistryProperties clientRegistryProperties,
                                           CacheAdapter<String, String> accessToken,
                                           CacheAdapter<String, String> refreshToken
    ) {
        return new CentralRegistry(clientRegistryClient, clientRegistryProperties, accessToken, refreshToken);
    }

    @Bean
    public ClientRegistryClient clientRegistryClient(WebClient.Builder builder,
                                                     ClientRegistryProperties clientRegistryProperties) {
        return new ClientRegistryClient(builder, clientRegistryProperties.getUrl());
    }

    @Bean
    public LockedUsersRepository lockedUsersRepository(DbOptions dbOptions) {
        return new LockedUsersRepository(pgPool(dbOptions));
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
    public DestinationsConfig destinationsConfig(AmqpAdmin amqpAdmin) {
        HashMap<String, DestinationsConfig.DestinationInfo> queues = new HashMap<>();
        queues.put(CONSENT_REQUEST_QUEUE, new DestinationsConfig.DestinationInfo("exchange", CONSENT_REQUEST_QUEUE));
        queues.put(HIU_CONSENT_NOTIFICATION_QUEUE,
                new DestinationsConfig.DestinationInfo("exchange", HIU_CONSENT_NOTIFICATION_QUEUE));
        queues.put(HIP_CONSENT_NOTIFICATION_QUEUE,
                new DestinationsConfig.DestinationInfo("exchange", HIP_CONSENT_NOTIFICATION_QUEUE));
        queues.put(HIP_DATA_FLOW_REQUEST_QUEUE,
                new DestinationsConfig.DestinationInfo("exchange", HIP_DATA_FLOW_REQUEST_QUEUE));

        Queue deadLetterQueue = QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
        Binding with = BindingBuilder
                .bind(deadLetterQueue)
                .to(new DirectExchange(CM_DEAD_LETTER_EXCHANGE))
                .with(CM_DEAD_LETTER_ROUTING_KEY);
        amqpAdmin.declareQueue(deadLetterQueue);
        amqpAdmin.declareExchange(new DirectExchange(CM_DEAD_LETTER_EXCHANGE));
        amqpAdmin.declareBinding(with);

        DestinationsConfig destinationsConfig = new DestinationsConfig(queues, null);
        destinationsConfig.getQueues()
                .forEach((key, destination) -> {
                    Exchange ex = ExchangeBuilder.directExchange(
                            destination.getExchange())
                            .durable(true)
                            .build();
                    amqpAdmin.declareExchange(ex);
                    Queue q = QueueBuilder.durable(
                            destination.getRoutingKey())
                            .deadLetterExchange(CM_DEAD_LETTER_EXCHANGE)
                            .deadLetterRoutingKey(CM_DEAD_LETTER_ROUTING_KEY)
                            .build();
                    amqpAdmin.declareQueue(q);
                    Binding b = BindingBuilder.bind(q)
                            .to(ex)
                            .with(destination.getRoutingKey())
                            .noargs();
                    amqpAdmin.declareBinding(b);
                });
        return destinationsConfig;
    }

    @Bean
    public IdentityService identityService(IdentityServiceClient identityServiceClient,
                                           IdentityServiceProperties identityServiceProperties) {
        return new IdentityService(identityServiceClient, identityServiceProperties);
    }

    @Bean
    public TokenService tokenService(IdentityServiceProperties identityServiceProperties,
                                     IdentityServiceClient identityServiceClient, UserRepository userRepository) {
        return new TokenService(identityServiceProperties, identityServiceClient, userRepository);
    }

    @Bean("pinSigning")
    public KeyPair keyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(2048);
        return keyPairGen.genKeyPair();
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
    public JWKSet jwkSet(ClientRegistryProperties clientRegistryProperties) throws IOException, ParseException {
        return JWKSet.load(new URL(clientRegistryProperties.getJwkUrl()));
    }

    @Bean("identityServiceJWKSet")
    public JWKSet identityServiceJWKSet(IdentityServiceProperties identityServiceProperties)
            throws IOException, ParseException {
        return JWKSet.load(new URL(identityServiceProperties.getJwkUrl()));
    }

    @Bean
    public CentralRegistryTokenVerifier centralRegistryTokenVerifier(@Qualifier("centralRegistryJWKSet") JWKSet jwkSet) {
        return new CentralRegistryTokenVerifier(jwkSet);
    }
}
