package in.org.projecteka.hdaf;

import in.org.projecteka.hdaf.clients.ClientRegistryClient;
import in.org.projecteka.hdaf.clients.HipServiceClient;
import in.org.projecteka.hdaf.clients.UserServiceClient;
import in.org.projecteka.hdaf.clients.properties.ClientRegistryProperties;
import in.org.projecteka.hdaf.clients.properties.HipServiceProperties;
import in.org.projecteka.hdaf.clients.properties.UserServiceProperties;
import in.org.projecteka.hdaf.link.discovery.Discovery;
import in.org.projecteka.hdaf.user.UserService;
import in.org.projecteka.hdaf.link.HIPClient;
import in.org.projecteka.hdaf.link.link.Link;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class HdafConfiguration {

    @Bean
    public Discovery discovery(WebClient.Builder builder,
                               ClientRegistryProperties clientRegistryProperties,
                               UserServiceProperties userServiceProperties,
                               HipServiceProperties hipServiceProperties) {
        ClientRegistryClient clientRegistryClient = new ClientRegistryClient(builder, clientRegistryProperties);
        UserServiceClient userServiceClient = new UserServiceClient(builder, userServiceProperties);
        HipServiceClient hipServiceClient = new HipServiceClient(builder, hipServiceProperties);

        return new Discovery(clientRegistryClient, userServiceClient, hipServiceClient);
    }

    @Bean
    public UserService userService() {
        return new UserService();
    }

    @Bean
    public Link link(WebClient.Builder builder, ClientRegistryProperties clientRegistryProperties) {
        return new Link(new HIPClient(builder), new ClientRegistryClient(builder, clientRegistryProperties));
    }
}
