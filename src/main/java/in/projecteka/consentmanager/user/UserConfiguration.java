package in.projecteka.consentmanager.user;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import in.projecteka.consentmanager.clients.IdentityServiceClient;
import in.projecteka.consentmanager.clients.OtpServiceClient;
import in.projecteka.consentmanager.clients.properties.IdentityServiceProperties;
import in.projecteka.consentmanager.clients.properties.OtpServiceProperties;
import in.projecteka.consentmanager.clients.properties.UserServiceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Configuration
public class UserConfiguration {

    @Bean
    public UserService userService(UserRepository userRepository,
                                   OtpServiceProperties otpServiceProperties,
                                   OtpServiceClient otpServiceClient,
                                   UserVerificationService userVerificationService,
                                   IdentityServiceClient identityServiceClient,
                                   TokenService tokenService) {
        return new UserService(userRepository,
                otpServiceProperties,
                otpServiceClient,
                userVerificationService,
                identityServiceClient,
                tokenService);
    }

    @Bean
    public UserRepository userRepository(WebClient.Builder builder, UserServiceProperties properties) {
        return new UserRepository(builder, properties);
    }

    @Bean
    public OtpServiceClient otpServiceClient(WebClient.Builder builder,
                                             OtpServiceProperties otpServiceProperties) {
        return new OtpServiceClient(builder, otpServiceProperties);
    }

    @Bean
    public IdentityServiceClient keycloakClient(WebClient.Builder builder,
                                                IdentityServiceProperties identityServiceProperties) {
        return new IdentityServiceClient(builder, identityServiceProperties);
    }

    @Bean
    public UserVerificationService authenticatorService(JWTProperties jwtProperties,
                                                        LoadingCache<String, Optional<String>> sessionCache,
                                                        LoadingCache<String, Optional<String>> secondSessionCache) {
        return new UserVerificationService(jwtProperties, sessionCache, secondSessionCache);
    }

    @Bean({"unverifiedSessions", "verifiedSessions"})
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

    @Bean
    public TokenService tokenService(IdentityServiceProperties identityServiceProperties, IdentityServiceClient identityServiceClient) {
        return new TokenService(identityServiceProperties, identityServiceClient);
    }
}
