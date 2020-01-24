package in.projecteka.consentmanager;

import in.projecteka.consentmanager.clients.ClientRegistryClient;
import in.projecteka.consentmanager.clients.DiscoveryServiceClient;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.clients.properties.ClientRegistryProperties;
import in.projecteka.consentmanager.clients.properties.UserServiceProperties;
import in.projecteka.consentmanager.consent.repository.ConsentRequestRepository;
import in.projecteka.consentmanager.consent.ConsentManager;
import in.projecteka.consentmanager.link.ClientErrorExceptionHandler;
import in.projecteka.consentmanager.link.HIPClient;
import in.projecteka.consentmanager.link.discovery.Discovery;
import in.projecteka.consentmanager.link.discovery.repository.DiscoveryRepository;
import in.projecteka.consentmanager.link.link.Link;
import in.projecteka.consentmanager.user.UserRepository;
import in.projecteka.consentmanager.user.UserService;
import in.projecteka.consentmanager.link.link.repository.LinkRepository;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ConsentManagerConfiguration {

    @Bean
    public Discovery discovery(WebClient.Builder builder,
                               ClientRegistryProperties clientRegistryProperties,
                               UserServiceProperties userServiceProperties,
                               DiscoveryRepository discoveryRepository) {
        ClientRegistryClient clientRegistryClient = new ClientRegistryClient(builder, clientRegistryProperties);
        UserServiceClient userServiceClient = new UserServiceClient(builder, userServiceProperties);
        DiscoveryServiceClient discoveryServiceClient = new DiscoveryServiceClient(builder);

        return new Discovery(clientRegistryClient, userServiceClient, discoveryServiceClient, discoveryRepository);
    }

    @Bean
    public PgPool pgPool(DbOptions dbOptions) {
        PgConnectOptions connectOptions = new PgConnectOptions()
                .setPort(dbOptions.getPort())
                .setHost(dbOptions.getHost())
                .setDatabase(dbOptions.getSchema())
                .setUser(dbOptions.getUser())
                .setPassword(dbOptions.getPassword());

        PoolOptions poolOptions = new PoolOptions()
                .setMaxSize(dbOptions.getPoolSize());

        return PgPool.pool(connectOptions, poolOptions);
    }

    @Bean
    public DiscoveryRepository discoveryRepository(PgPool pgPool) {
        return new DiscoveryRepository(pgPool);
    }

    @Bean
    public LinkRepository linkRepository(PgPool pgPool) {
        return new LinkRepository(pgPool);
    }

    @Bean
    public UserService userService(UserRepository userRepository) {
        return new UserService(userRepository);
    }

    @Bean
    public UserRepository userRepository(WebClient.Builder builder, UserServiceProperties properties) {
        return new UserRepository(builder, properties);
    }

    @Bean
    public Link link(WebClient.Builder builder,
                     ClientRegistryProperties clientRegistryProperties,
                     LinkRepository linkRepository,
                     UserServiceProperties userServiceProperties) {
        return new Link(new HIPClient(builder),
                        new ClientRegistryClient(builder, clientRegistryProperties),
                        linkRepository, new UserServiceClient(builder, userServiceProperties));
    }

    @Bean
    // This exception handler needs to be given highest priority compared to DefaultErrorWebExceptionHandler, hence order = -2.
    @Order(-2)
    public ClientErrorExceptionHandler clientErrorExceptionHandler(ErrorAttributes errorAttributes,
                                                                   ResourceProperties resourceProperties,
                                                                   ApplicationContext applicationContext,
                                                                   ServerCodecConfigurer serverCodecConfigurer) {

        ClientErrorExceptionHandler clientErrorExceptionHandler = new ClientErrorExceptionHandler(errorAttributes,
                resourceProperties, applicationContext);
        clientErrorExceptionHandler.setMessageWriters(serverCodecConfigurer.getWriters());
        return clientErrorExceptionHandler;
    }

    @Bean
    public ConsentRequestRepository consentRequestRepository(PgPool pgPool) {
        return new ConsentRequestRepository(pgPool);
    }

    @Bean
    public ConsentManager consentRequestService(WebClient.Builder builder,
                                                ConsentRequestRepository repository,
                                                ClientRegistryProperties clientRegistryProperties,
                                                UserServiceProperties userServiceProperties) {
        return new ConsentManager(repository,
                new ClientRegistryClient(builder, clientRegistryProperties),
                new UserServiceClient(builder, userServiceProperties));
    }
}
