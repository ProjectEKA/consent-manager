package in.projecteka.consentmanager;

import in.projecteka.consentmanager.clients.properties.ClientRegistryProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ClientRegistryProperties.class)
public class ConsentManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsentManagerApplication.class, args);
    }
}
