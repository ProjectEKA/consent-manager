package in.projecteka.consentmanager.user;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import in.projecteka.consentmanager.clients.HASSignupServiceClient;
import in.projecteka.consentmanager.clients.HealthAccountServiceClient;
import in.projecteka.consentmanager.clients.IdentityServiceClient;
import in.projecteka.consentmanager.clients.OtpServiceClient;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.clients.properties.HealthAccountServiceProperties;
import in.projecteka.consentmanager.clients.properties.IdentityServiceProperties;
import in.projecteka.consentmanager.clients.properties.OtpServiceProperties;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import in.projecteka.consentmanager.common.cache.LoadingCacheAdapter;
import in.projecteka.consentmanager.common.cache.RedisCacheAdapter;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import in.projecteka.consentmanager.consent.ConsentServiceProperties;
import io.vertx.pgclient.PgPool;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.security.PrivateKey;
import java.util.concurrent.TimeUnit;

@Configuration
public class UserConfiguration {

    @Bean
    public UserService userService(UserRepository userRepository,
                                   OtpServiceProperties otpServiceProperties,
                                   OtpServiceClient otpServiceClient,
                                   HealthAccountServiceProperties healthAccountServiceProperties,
                                   HealthAccountServiceClient healthAccountServiceClient,
                                   SignUpService signupService,
                                   IdentityServiceClient identityServiceClient,
                                   TokenService tokenService,
                                   UserServiceProperties userServiceProperties,
                                   OtpAttemptService otpAttemptService,
                                   LockedUserService lockedUserService,
                                   UserServiceClient userServiceClient,
                                   ConsentServiceProperties consentServiceProperties) {
        return new UserService(userRepository,
                otpServiceProperties,
                otpServiceClient,
                healthAccountServiceProperties,
                healthAccountServiceClient,
                signupService,
                identityServiceClient,
                tokenService,
                userServiceProperties,
                otpAttemptService,
                lockedUserService,
                userServiceClient,
                consentServiceProperties);
    }

    @Bean
    public UserRepository userRepository(PgPool pgPool) {
        return new UserRepository(pgPool);
    }

    @Bean
    public ReactorClientHttpConnector reactorClientHttpConnector() {
        HttpClient httpClient = null;
        try {
            SslContext sslContext = SslContextBuilder
                    .forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
            httpClient = HttpClient.create().secure(t -> t.sslContext(sslContext) );
        } catch (SSLException e) {
            e.printStackTrace();
        }
        return new ReactorClientHttpConnector(httpClient);
    }

    @Bean
    public HealthAccountServiceClient healthAccountServiceClient(@Qualifier("customBuilder") WebClient.Builder builder,
                                                       HealthAccountServiceProperties healthAccountServiceProperties) {
        if (healthAccountServiceProperties.isUsingUnsecureSSL()){
            builder.clientConnector(reactorClientHttpConnector());
        }
        return new HealthAccountServiceClient(builder, healthAccountServiceProperties.getUrl());
    }

    @Bean
    public IdentityServiceClient keycloakClient(@Qualifier("customBuilder") WebClient.Builder builder,
                                                IdentityServiceProperties identityServiceProperties) {
        return new IdentityServiceClient(builder, identityServiceProperties);
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

    @ConditionalOnProperty(value = "consentmanager.cacheMethod", havingValue = "guava", matchIfMissing = true)
    @Bean({"unverifiedSessions", "verifiedSessions", "blockListedTokens", "usedTokens", "hasCache"})
    public CacheAdapter<String, String> createLoadingCacheAdapter() {
        return new LoadingCacheAdapter(createSessionCache(5));
    }

    @ConditionalOnProperty(value = "consentmanager.cacheMethod", havingValue = "guava", matchIfMissing = true)
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

    @ConditionalOnProperty(value = "consentmanager.cacheMethod", havingValue = "redis")
    @Bean({"unverifiedSessions", "verifiedSessions", "blockListedTokens", "usedTokens", "hasCache"})
    public CacheAdapter<String, String> createRedisCacheAdapter(ReactiveRedisOperations<String, String> redisClient) {
        return new RedisCacheAdapter(redisClient, 5);
    }

    @ConditionalOnProperty(value = "consentmanager.cacheMethod", havingValue = "redis")
    @Bean({"dayCache"})
    public CacheAdapter<String, String> createDayRedisCacheAdapter(
            ReactiveRedisOperations<String, String> stringReactiveRedisOperations) {
        return new RedisCacheAdapter(stringReactiveRedisOperations, 24 * 60);
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
            OtpAttemptService otpAttemptService,
            ConsentServiceProperties serviceProperties) {
        return new SessionService(tokenService,
                blockListedTokens,
                unverifiedSessions,
                lockedUserService,
                userRepository,
                otpServiceClient,
                otpServiceProperties,
                otpAttemptService,
                serviceProperties);
    }

    @Bean
    public HASSignupServiceClient hasSignupServiceClient(@Qualifier("customBuilder") WebClient.Builder builder,
                                                         HealthAccountServiceProperties healthAccountServiceProperties) {
        return new HASSignupServiceClient(builder, healthAccountServiceProperties.getUrl());
    }

    @Bean
    public DummyHealthAccountService dummyHealthAccountService(UserRepository userRepository) {
        return new DummyHealthAccountService(userRepository);
    }

    @Bean
    public HASSignupService hasSignupService(HASSignupServiceClient hasSignupServiceClient,
                                             UserRepository userRepository,
                                             SignUpService signUpService,
                                             TokenService tokenService,
                                             IdentityServiceClient identityServiceClient,
                                             SessionService sessionService,
                                             OtpServiceProperties otpServiceProperties,
                                             DummyHealthAccountService dummyHealthAccountService,
                                             CacheAdapter<String, String> hasCache
    ) {
        return new HASSignupService(hasSignupServiceClient,
                userRepository,
                signUpService,
                tokenService,
                identityServiceClient,
                sessionService,
                otpServiceProperties,
                dummyHealthAccountService,
                hasCache);
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
        return new TransactionPinService(transactionPinRepository,
                encoder,
                privateKey,
                userServiceProperties,
                dayCache);
    }

    @Bean
    public ProfileService profileService(UserService userService, TransactionPinService transactionPinService) {
        return new ProfileService(userService, transactionPinService);
    }
}
