package in.projecteka.consentmanager.user;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import in.projecteka.consentmanager.clients.IdentityServiceClient;
import in.projecteka.consentmanager.clients.OtpServiceClient;
import in.projecteka.consentmanager.clients.properties.IdentityServiceProperties;
import in.projecteka.consentmanager.clients.properties.OtpServiceProperties;
import in.projecteka.consentmanager.common.cache.ICacheAdapter;
import in.projecteka.consentmanager.common.cache.LoadingCacheAdapter;
import in.projecteka.consentmanager.common.cache.RedisCacheAdapter;
import in.projecteka.consentmanager.common.cache.RedisOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.vertx.pgclient.PgPool;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.PrivateKey;
import java.util.Optional;
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
                                   UserServiceProperties userServiceProperties) {
        return new UserService(userRepository,
                otpServiceProperties,
                otpServiceClient,
                signupService,
                identityServiceClient,
                tokenService,
                userServiceProperties);
    }

    @Bean
    public UserRepository userRepository(PgPool pgPool) {
        return new UserRepository(pgPool);
    }

    @Bean
    public OtpServiceClient otpServiceClient(WebClient.Builder builder,
                                             OtpServiceProperties otpServiceProperties) {
        return new OtpServiceClient(builder, otpServiceProperties.getUrl());
    }

    @Bean
    public IdentityServiceClient keycloakClient(WebClient.Builder builder,
                                                IdentityServiceProperties identityServiceProperties) {
        return new IdentityServiceClient(builder, identityServiceProperties);
    }

    @Bean
    public SignUpService authenticatorService(JWTProperties jwtProperties,
                                              ICacheAdapter<String, Optional<String>> sessionCache,
                                              ICacheAdapter<String, Optional<String>> secondSessionCache,
                                              UserServiceProperties userServiceProperties) {
        return new SignUpService(jwtProperties,
                sessionCache,
                secondSessionCache,
                userServiceProperties.getUserCreationTokenValidity());
    }

    @Profile("default")
    @Bean({"unverifiedSessions", "verifiedSessions"})
    public ICacheAdapter<String, Optional<String>> createLoadingCacheAdapter(LoadingCache<String,Optional<String>> cache) {
       return new LoadingCacheAdapter(cache);
    }

    @Bean
    @Profile("default")
    public LoadingCache<String, Optional<String>> createSessionCache() {
        return CacheBuilder
                .newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    public Optional<String> load(String key) {
                        return Optional.empty();
                    }
                });
    }

    @Profile("redis")
    @Bean({"unverifiedSessions", "verifiedSessions"})
    public ICacheAdapter<String, Optional<String>> createRedisCacheAdapter(RedisOptions redisOptions) {
        RedisURI redisUri = RedisURI.Builder.
                redis(redisOptions.getHost())
                .withPort(redisOptions.getPort())
                .build();
        RedisClient redisClient = RedisClient.create(redisUri);
        return new RedisCacheAdapter(redisClient);
    }


    @Bean
    public SessionService sessionService(TokenService tokenService) {
        return new SessionService(tokenService);
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
                                                       UserServiceProperties userServiceProperties) {
        return new TransactionPinService(transactionPinRepository, encoder, privateKey, userServiceProperties);
    }

    @Bean
    public ProfileService profileService(UserService userService, TransactionPinService transactionPinService) {
        return new ProfileService(userService, transactionPinService);
    }
}
