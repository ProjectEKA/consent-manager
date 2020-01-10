package in.projecteka.hdaf;

import in.projecteka.hdaf.clients.properties.ClientRegistryProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ClientRegistryProperties.class)
public class HdafApplication {

    public static void main(String[] args) {
        SpringApplication.run(HdafApplication.class, args);
    }
}
