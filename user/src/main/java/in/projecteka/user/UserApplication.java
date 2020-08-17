package in.projecteka.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        LockedServiceProperties.class,
        JWTProperties.class,
        OtpServiceProperties.class,
        IdentityServiceProperties.class,
        GatewayServiceProperties.class,
        RedisOptions.class
})
public class UserApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }

}
