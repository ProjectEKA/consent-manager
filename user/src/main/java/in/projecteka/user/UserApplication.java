package in.projecteka.user;

import in.projecteka.user.properties.DbOptions;
import in.projecteka.user.properties.GatewayServiceProperties;
import in.projecteka.user.properties.IdentityServiceProperties;
import in.projecteka.user.properties.JWTProperties;
import in.projecteka.user.properties.KeyPairConfig;
import in.projecteka.user.properties.LockedServiceProperties;
import in.projecteka.user.properties.OtpServiceProperties;
import in.projecteka.user.properties.RedisOptions;
import in.projecteka.user.properties.UserServiceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(exclude = {RedisAutoConfiguration.class})
@EnableConfigurationProperties({
        LockedServiceProperties.class,
        JWTProperties.class,
        OtpServiceProperties.class,
        IdentityServiceProperties.class,
        GatewayServiceProperties.class,
        RedisOptions.class,
        DbOptions.class,
        UserServiceProperties.class,
        WebClientOptions.class,
        KeyPairConfig.class
})
public class UserApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }
}
