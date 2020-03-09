package in.projecteka.consentmanager.link;

import in.projecteka.consentmanager.clients.DiscoveryServiceClient;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.clients.properties.UserServiceProperties;
import in.projecteka.consentmanager.common.CentralRegistry;
import in.projecteka.consentmanager.link.discovery.Discovery;
import in.projecteka.consentmanager.link.discovery.DiscoveryRepository;
import in.projecteka.consentmanager.link.link.Link;
import in.projecteka.consentmanager.link.link.LinkRepository;
import io.vertx.pgclient.PgPool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class LinkConfiguration {

    @Bean
    public DiscoveryRepository discoveryRepository(PgPool pgPool) {
        return new DiscoveryRepository(pgPool);
    }

    @Bean
    public LinkRepository linkRepository(PgPool pgPool) {
        return new LinkRepository(pgPool);
    }

    @Bean
    public Link link(WebClient.Builder builder,
                     LinkRepository linkRepository,
                     UserServiceProperties userServiceProperties,
                     CentralRegistry centralRegistry) {
        return new Link(new HIPClient(builder),
                linkRepository,
                new UserServiceClient(builder, userServiceProperties),
                centralRegistry);
    }

    @Bean
    public Discovery discovery(WebClient.Builder builder,
                               UserServiceProperties userServiceProperties,
                               DiscoveryRepository discoveryRepository,
                               CentralRegistry centralRegistry) {
        UserServiceClient userServiceClient = new UserServiceClient(builder, userServiceProperties);
        DiscoveryServiceClient discoveryServiceClient = new DiscoveryServiceClient(builder);
        return new Discovery(userServiceClient, discoveryServiceClient, discoveryRepository, centralRegistry);
    }
}
