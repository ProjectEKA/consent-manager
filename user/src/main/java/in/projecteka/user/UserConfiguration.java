package in.projecteka.user;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import in.projecteka.library.clients.IdentityServiceClient;
import in.projecteka.library.clients.OtpServiceClient;
import in.projecteka.library.common.cache.CacheAdapter;
import in.projecteka.library.common.cache.LoadingCacheAdapter;
import in.projecteka.library.common.cache.RedisCacheAdapter;
import in.projecteka.user.clients.UserServiceClient;
import io.vertx.pgclient.PgPool;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.PrivateKey;
import java.util.concurrent.TimeUnit;

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
    public CacheAdapter<String, String> createRedisCacheAdapter(
            ReactiveRedisOperations<String, String> stringReactiveRedisOperations,
            RedisOptions redisOptions) {
        return new RedisCacheAdapter(stringReactiveRedisOperations, 5, redisOptions.getRetry());
    }

    @ConditionalOnProperty(value = "consentmanager.cacheMethod", havingValue = "redis")
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
}
