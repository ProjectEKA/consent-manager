package in.projecteka.consentmanager;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.consentmanager.clients.ClientRegistryClient;
import in.projecteka.consentmanager.clients.IdentityServiceClient;
import in.projecteka.consentmanager.clients.ServiceAuthenticationClient;
import in.projecteka.consentmanager.clients.properties.ClientRegistryProperties;
import in.projecteka.consentmanager.clients.properties.GatewayServiceProperties;
import in.projecteka.consentmanager.clients.properties.IdentityServiceProperties;
import in.projecteka.consentmanager.common.CentralRegistry;
import in.projecteka.consentmanager.common.GatewayTokenVerifier;
import in.projecteka.consentmanager.common.IdentityService;
import in.projecteka.consentmanager.common.ServiceAuthentication;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import in.projecteka.consentmanager.common.cache.LoadingCacheAdapter;
import in.projecteka.consentmanager.common.cache.RedisCacheAdapter;
import in.projecteka.consentmanager.common.cache.RedisOptions;
import in.projecteka.consentmanager.common.heartbeat.Heartbeat;
import in.projecteka.consentmanager.common.heartbeat.RabbitmqOptions;
import in.projecteka.consentmanager.link.ClientErrorExceptionHandler;
import in.projecteka.consentmanager.user.LockedUsersRepository;
import in.projecteka.consentmanager.user.TokenService;
import in.projecteka.consentmanager.user.UserRepository;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
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
    public static final String PARKING_EXCHANGE = "parking.exchange";
    public static final String EXCHANGE = "exchange";

    @ConditionalOnProperty(value = "consentmanager.cacheMethod", havingValue = "guava", matchIfMissing = true)
    @Bean({"accessToken"})
    public CacheAdapter<String, String> createLoadingCacheAdapterForAccessToken() {
        return new LoadingCacheAdapter(createSessionCache(5));
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
    @Bean
    public RedisClient getRedisClient(RedisOptions redisOptions) {
        RedisURI redisUri = RedisURI.Builder.
                redis(redisOptions.getHost())
                .withPort(redisOptions.getPort())
                .withPassword(redisOptions.getPassword())
                .build();
        return RedisClient.create(redisUri);
    }

    @ConditionalOnProperty(value = "consentmanager.cacheMethod", havingValue = "redis")
    @Bean({"accessToken"})
    public CacheAdapter<String, String> createRedisCacheAdapter(RedisClient redisClient) {
        return new RedisCacheAdapter(redisClient, 5);
    }

    @Bean
    public CentralRegistry centralRegistry(ClientRegistryClient clientRegistryClient) {
        return new CentralRegistry(clientRegistryClient);
    }

    @Bean
    public ServiceAuthenticationClient serviceAuthenticationClient(WebClient.Builder webClientBuilder,
                                                                   GatewayServiceProperties gatewayServiceProperties) {
        return new ServiceAuthenticationClient(webClientBuilder, gatewayServiceProperties.getBaseUrl());
    }

    @Bean
    public ServiceAuthentication serviceAuthentication(ServiceAuthenticationClient serviceAuthenticationClient,
                                                       GatewayServiceProperties gatewayServiceProperties,
                                                       CacheAdapter<String, String> accessToken) {
        return new ServiceAuthentication(serviceAuthenticationClient, gatewayServiceProperties, accessToken);
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
    public DestinationsConfig destinationsConfig() {
        HashMap<String, DestinationsConfig.DestinationInfo> queues = new HashMap<>();
        queues.put(CONSENT_REQUEST_QUEUE,
                new DestinationsConfig.DestinationInfo(EXCHANGE, CONSENT_REQUEST_QUEUE));
        queues.put(HIU_CONSENT_NOTIFICATION_QUEUE,
                new DestinationsConfig.DestinationInfo(EXCHANGE, HIU_CONSENT_NOTIFICATION_QUEUE));
        queues.put(HIP_CONSENT_NOTIFICATION_QUEUE,
                new DestinationsConfig.DestinationInfo(EXCHANGE, HIP_CONSENT_NOTIFICATION_QUEUE));
        queues.put(HIP_DATA_FLOW_REQUEST_QUEUE,
                new DestinationsConfig.DestinationInfo(EXCHANGE, HIP_DATA_FLOW_REQUEST_QUEUE));
        return new DestinationsConfig(queues);
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
    public GatewayTokenVerifier centralRegistryTokenVerifier(
            @Qualifier("centralRegistryJWKSet") JWKSet jwkSet) {
        return new GatewayTokenVerifier(jwkSet);
    }

    @Bean
    public Heartbeat heartbeat(IdentityServiceProperties identityServiceProperties,
                               DbOptions dbOptions,
                               RabbitmqOptions rabbitmqOptions) {
        return new Heartbeat(identityServiceProperties, dbOptions, rabbitmqOptions);
    }
}
