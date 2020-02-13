package in.projecteka.consentmanager.user;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
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
                                   OtpServiceClient otpServiceClient) {
        return new UserService(userRepository, otpServiceProperties, otpServiceClient);
    }

    @Bean
    public UserRepository userRepository(WebClient.Builder builder, UserServiceProperties properties) {
        return new UserRepository(builder, properties);
    }

    @Bean
    public OtpServiceClient otpServiceClient(WebClient.Builder builder,
                                             OtpServiceProperties otpServiceProperties,
                                             AuthenticatorService authenticatorService) {
        return new OtpServiceClient(builder, otpServiceProperties, authenticatorService);
    }

    @Bean
    public AuthenticatorService authenticatorService(JWTProperties jwtProperties,
                                                     LoadingCache<String, Optional<String>> sessionCache,
                                                     LoadingCache<String, Optional<String>> secondSessionCache) {
        return new AuthenticatorService(jwtProperties, sessionCache, secondSessionCache);
    }

    @Bean({ "sessionCache", "secondSessionCache" })
    public LoadingCache<String, Optional<String>> createSessionCache() {
        return CacheBuilder.newBuilder().
                expireAfterWrite(5, TimeUnit.MINUTES).build(new CacheLoader<>() {
            public Optional<String> load(String key) {
                return Optional.empty();
            }
        });
    }
}
