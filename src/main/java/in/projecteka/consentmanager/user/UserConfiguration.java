package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.properties.UserServiceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

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
    public OtpServiceClient otpServiceClient(WebClient.Builder builder, OtpServiceProperties otpServiceProperties) {
        return new OtpServiceClient(builder, otpServiceProperties);
    }
}
