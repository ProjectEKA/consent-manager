package in.org.projecteka.hdaf;

import in.org.projecteka.hdaf.link.ClientRegistryClientImpl;
import in.org.projecteka.hdaf.link.ClientRegistryProperties;
import in.org.projecteka.hdaf.link.discovery.Discovery;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class HdafConfiguration {

    @Bean
    public Discovery discovery(WebClient.Builder builder, ClientRegistryProperties clientRegistryProperties) {
        return new Discovery(new ClientRegistryClientImpl(builder, clientRegistryProperties));
    }
}
