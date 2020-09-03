package in.projecteka.dataflow;

import in.projecteka.dataflow.properties.DataFlowConsentManagerProperties;
import in.projecteka.dataflow.properties.DbOptions;
import in.projecteka.dataflow.properties.GatewayServiceProperties;
import in.projecteka.dataflow.properties.IdentityServiceProperties;
import in.projecteka.dataflow.properties.RedisOptions;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(exclude = {RedisAutoConfiguration.class})
@EnableConfigurationProperties({DataFlowConsentManagerProperties.class,
        GatewayServiceProperties.class,
        DbOptions.class,
        IdentityServiceProperties.class,
        RedisOptions.class})
public class DataFlowApplication {
    public static void main(String[] args) {
        SpringApplication.run(DataFlowApplication.class, args);
    }
}
