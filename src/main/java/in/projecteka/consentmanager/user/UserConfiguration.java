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
import io.lettuce.core.RedisClient;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.vertx.pgclient.PgPool;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
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
                                   UserServiceClient userServiceClient) {
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
                userServiceClient);
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
    public OtpServiceClient otpServiceClient(@Qualifier("customBuilder") WebClient.Builder builder,
                                             OtpServiceProperties otpServiceProperties) {
        return new OtpServiceClient(builder, otpServiceProperties.getUrl());
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
    public SignUpService authenticatorService(JWTProperties jwtProperties,
                                              CacheAdapter<String, String> unverifiedSessions,
                                              CacheAdapter<String, String> verifiedSessions,
                                              UserServiceProperties userServiceProperties) {
        return new SignUpService(jwtProperties,
                unverifiedSessions,
                verifiedSessions,
                userServiceProperties.getUserCreationTokenValidity());
    }

    @ConditionalOnProperty(value = "consentmanager.cacheMethod", havingValue = "guava", matchIfMissing = true)
    @Bean({"unverifiedSessions", "verifiedSessions", "blockListedTokens", "usedTokens"})
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
    @Bean({"unverifiedSessions", "verifiedSessions", "blockListedTokens", "usedTokens"})
    public CacheAdapter<String, String> createRedisCacheAdapter(RedisClient redisClient) {
        return new RedisCacheAdapter(redisClient, 5);
    }

    @ConditionalOnProperty(value = "consentmanager.cacheMethod", havingValue = "redis")
    @Bean({"dayCache"})
    public CacheAdapter<String, String> createDayRedisCacheAdapter(RedisClient redisClient) {
        return new RedisCacheAdapter(redisClient, 24 * 60);
    }

    @Bean
    LockedUserService lockedUserService(LockedUsersRepository lockedUsersRepository, LockedServiceProperties lockedServiceProperties) {
        return new LockedUserService(lockedUsersRepository, lockedServiceProperties);
    }

    @Bean
    public SessionService sessionService(TokenService tokenService,
                                         CacheAdapter<String, String> blockListedTokens,
                                         CacheAdapter<String, String> unverifiedSessions,
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
    public HASSignupServiceClient hasSignupServiceClient(@Qualifier("customBuilder") WebClient.Builder builder,
                                                         HealthAccountServiceProperties healthAccountServiceProperties) {
        return new HASSignupServiceClient(builder, healthAccountServiceProperties.getUrl());
    }

    @Bean
    public HASSignupService hasSignupService(HASSignupServiceClient hasSignupServiceClient,
                                             UserRepository userRepository,
                                             SignUpService signUpService,
                                             TokenService tokenService,
                                             IdentityServiceClient identityServiceClient,
                                             SessionService sessionService,
                                             BCryptPasswordEncoder bCryptPasswordEncoder) {
        return new HASSignupService(hasSignupServiceClient,
                userRepository,
                signUpService,
                tokenService,
                identityServiceClient,
                sessionService,
                bCryptPasswordEncoder);
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
                                                       CacheAdapter<String, String> dayCache) {
        return new TransactionPinService(transactionPinRepository, encoder, privateKey, userServiceProperties, dayCache);
    }

    @Bean
    public ProfileService profileService(UserService userService, TransactionPinService transactionPinService) {
        return new ProfileService(userService, transactionPinService);
    }
}
