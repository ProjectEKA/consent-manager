package in.projecteka.consentmanager;

import in.projecteka.consentmanager.properties.ClientRegistryProperties;
import in.projecteka.consentmanager.properties.GatewayServiceProperties;
import in.projecteka.consentmanager.properties.IdentityServiceProperties;
import in.projecteka.consentmanager.properties.LinkServiceProperties;
import in.projecteka.consentmanager.properties.LinkTokenCacheProperties;
import in.projecteka.consentmanager.properties.OtpServiceProperties;
import in.projecteka.consentmanager.properties.KeyPairConfig;
import in.projecteka.consentmanager.properties.ListenerProperties;
import in.projecteka.consentmanager.properties.RedisOptions;
import in.projecteka.consentmanager.properties.CacheMethodProperty;
import in.projecteka.consentmanager.properties.RabbitmqOptions;
import in.projecteka.consentmanager.consent.ConsentServiceProperties;
import in.projecteka.consentmanager.consent.NHSProperties;
import in.projecteka.consentmanager.dataflow.DataFlowConsentManagerProperties;
import in.projecteka.consentmanager.user.JWTProperties;
import in.projecteka.consentmanager.user.LockedServiceProperties;
import in.projecteka.consentmanager.user.UserServiceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {RedisAutoConfiguration.class})
@EnableScheduling
@EnableAsync
@EnableConfigurationProperties({ClientRegistryProperties.class,
                                DbOptions.class,
                                DataFlowConsentManagerProperties.class,
                                OtpServiceProperties.class,
                                IdentityServiceProperties.class,
                                LinkServiceProperties.class,
                                UserServiceProperties.class,
                                LockedServiceProperties.class,
                                JWTProperties.class,
                                ConsentServiceProperties.class,
                                RedisOptions.class,
                                ListenerProperties.class,
                                GatewayServiceProperties.class,
                                RabbitmqOptions.class,
                                NHSProperties.class,
                                KeyPairConfig.class,
                                CacheMethodProperty.class,
                                LinkTokenCacheProperties.class
})
public class ConsentManagerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConsentManagerApplication.class, args);
    }
}
