package in.projecteka.consentmanager.link;

import in.projecteka.consentmanager.clients.DiscoveryServiceClient;
import in.projecteka.consentmanager.clients.LinkServiceClient;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.common.CentralRegistry;
import in.projecteka.consentmanager.common.IdentityService;
import in.projecteka.consentmanager.link.discovery.Discovery;
import in.projecteka.consentmanager.link.discovery.DiscoveryRepository;
import in.projecteka.consentmanager.link.discovery.GatewayServiceProperties;
import in.projecteka.consentmanager.link.link.Link;
import in.projecteka.consentmanager.link.link.LinkRepository;
import in.projecteka.consentmanager.user.UserServiceProperties;
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
    public Link link(WebClient.Builder builder, LinkRepository linkRepository, CentralRegistry centralRegistry) {
        return new Link(new LinkServiceClient(builder), linkRepository, centralRegistry);
    }

    @Bean
    public DiscoveryServiceClient discoveryServiceClient(WebClient.Builder builder,
                                                         CentralRegistry centralRegistry) {
        return new DiscoveryServiceClient(builder, centralRegistry::authenticate);
    }

    @Bean
    public UserServiceClient userServiceClient(WebClient.Builder builder,
                                               UserServiceProperties userServiceProperties,
                                               IdentityService identityService) {
        return new UserServiceClient(builder, userServiceProperties.getUrl(), identityService::authenticate);
    }

    @Bean
    public Discovery discovery(DiscoveryRepository discoveryRepository,
                               CentralRegistry centralRegistry,
                               DiscoveryServiceClient discoveryServiceClient,
                               UserServiceClient userServiceClient,
                               GatewayServiceProperties gatewayServiceProperties) {
        return new Discovery(userServiceClient, discoveryServiceClient, discoveryRepository, centralRegistry, gatewayServiceProperties);
    }
}
