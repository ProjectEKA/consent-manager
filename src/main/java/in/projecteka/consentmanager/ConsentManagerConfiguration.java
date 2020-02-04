package in.projecteka.consentmanager;

import in.projecteka.consentmanager.clients.ClientRegistryClient;
import in.projecteka.consentmanager.clients.ConsentArtefactNotifier;
import in.projecteka.consentmanager.clients.ConsentManagerClient;
import in.projecteka.consentmanager.clients.DiscoveryServiceClient;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.clients.DataRequestNotifier;
import in.projecteka.consentmanager.clients.properties.ClientRegistryProperties;
import in.projecteka.consentmanager.clients.properties.UserServiceProperties;
import in.projecteka.consentmanager.consent.ConsentArtefactBroadcastListener;
import in.projecteka.consentmanager.consent.ConsentManager;
import in.projecteka.consentmanager.consent.PostConsentApproval;
import in.projecteka.consentmanager.consent.repository.ConsentArtefactRepository;
import in.projecteka.consentmanager.consent.repository.ConsentRequestRepository;
import in.projecteka.consentmanager.dataflow.DataFlowConsentManagerProperties;
import in.projecteka.consentmanager.dataflow.DataFlowRequest;
import in.projecteka.consentmanager.dataflow.DataFlowRequestRepository;
import in.projecteka.consentmanager.dataflow.DataFlowAuthServerProperties;
import in.projecteka.consentmanager.dataflow.PostDataFlowRequestApproval;
import in.projecteka.consentmanager.dataflow.DataFlowBroadcastListener;
import in.projecteka.consentmanager.link.ClientErrorExceptionHandler;
import in.projecteka.consentmanager.link.HIPClient;
import in.projecteka.consentmanager.link.discovery.Discovery;
import in.projecteka.consentmanager.link.discovery.repository.DiscoveryRepository;
import in.projecteka.consentmanager.link.link.Link;
import in.projecteka.consentmanager.link.link.repository.LinkRepository;
import in.projecteka.consentmanager.user.UserRepository;
import in.projecteka.consentmanager.user.UserService;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import lombok.SneakyThrows;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.HashMap;

@Configuration
public class ConsentManagerConfiguration {
    public static final String CONSENT_GRANTED_QUEUE = "hiu-notification-queue";
    public static final String HIP_DATA_FLOW_REQUEST_QUEUE = "hip-data-flow-request-queue";

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
    public ConsentArtefactRepository consentArtefactRepository(PgPool pgPool) {
        return new ConsentArtefactRepository(pgPool);
    }

    @Bean
    public PostConsentApproval postConsentApproval(AmqpTemplate amqpTemplate, DestinationsConfig destinationsConfig) {
        return new PostConsentApproval(amqpTemplate, destinationsConfig);
    }

    @Bean
    public ConsentManager consentRequestService(WebClient.Builder builder,
                                                ConsentRequestRepository repository,
                                                ClientRegistryProperties clientRegistryProperties,
                                                UserServiceProperties userServiceProperties,
                                                ConsentArtefactRepository consentArtefactRepository,
                                                KeyPair keyPair,
                                                PostConsentApproval postConsentApproval) {
        return new ConsentManager(
                new ClientRegistryClient(builder, clientRegistryProperties),
                new UserServiceClient(builder, userServiceProperties),
                repository,
                consentArtefactRepository,
                keyPair,
                postConsentApproval);
    }

    @Bean
    public DestinationsConfig destinationsConfig(AmqpAdmin amqpAdmin) {
        HashMap<String, DestinationsConfig.DestinationInfo> queues = new HashMap<>();
        queues.put(CONSENT_GRANTED_QUEUE, new DestinationsConfig.DestinationInfo("exchange", CONSENT_GRANTED_QUEUE));
        queues.put(HIP_DATA_FLOW_REQUEST_QUEUE, new DestinationsConfig.DestinationInfo("exchange", HIP_DATA_FLOW_REQUEST_QUEUE));

        DestinationsConfig destinationsConfig = new DestinationsConfig(queues, null);
        destinationsConfig.getQueues()
                .forEach((key, destination) -> {
                    Exchange ex = ExchangeBuilder.directExchange(
                            destination.getExchange())
                            .durable(true)
                            .build();
                    amqpAdmin.declareExchange(ex);
                    Queue q = QueueBuilder.durable(
                            destination.getRoutingKey())
                            .build();
                    amqpAdmin.declareQueue(q);
                    Binding b = BindingBuilder.bind(q)
                            .to(ex)
                            .with(destination.getRoutingKey())
                            .noargs();
                    amqpAdmin.declareBinding(b);
                });

        return destinationsConfig;
    }

    @Bean
    public Jackson2JsonMessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public MessageListenerContainerFactory messageListenerContainerFactory(ConnectionFactory connectionFactory,
                                                                           Jackson2JsonMessageConverter jackson2JsonMessageConverter) {
        return new MessageListenerContainerFactory(connectionFactory, jackson2JsonMessageConverter);
    }

    @Bean
    public ConsentArtefactNotifier consentArtefactClient(WebClient.Builder builder) {
        return new ConsentArtefactNotifier(builder);
    }

    @Bean
    public ConsentArtefactBroadcastListener hiuNotificationListener(MessageListenerContainerFactory messageListenerContainerFactory,
                                                                    DestinationsConfig destinationsConfig,
                                                                    Jackson2JsonMessageConverter jackson2JsonMessageConverter,
                                                                    ConsentArtefactNotifier consentArtefactNotifier) {
        return new ConsentArtefactBroadcastListener(
                messageListenerContainerFactory,
                destinationsConfig,
                jackson2JsonMessageConverter,
                consentArtefactNotifier);
    }

    @SneakyThrows
    @Bean
    public KeyPair keyPair() {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }

    @Bean
    public PostDataFlowRequestApproval postDataFlowRequestApproval(AmqpTemplate amqpTemplate,
                                                                   DestinationsConfig destinationsConfig) {
        return new PostDataFlowRequestApproval(amqpTemplate, destinationsConfig);
    }

    @Bean
    public DataFlowBroadcastListener dataFlowBroadcastListener(MessageListenerContainerFactory messageListenerContainerFactory,
                                                               DestinationsConfig destinationsConfig,
                                                               Jackson2JsonMessageConverter jackson2JsonMessageConverter,
                                                               DataRequestNotifier dataRequestNotifier,
                                                               DataFlowRequestRepository dataFlowRequestRepository,
                                                               WebClient.Builder builder,
                                                               ClientRegistryProperties clientRegistryProperties) {
        return new DataFlowBroadcastListener(messageListenerContainerFactory,
                destinationsConfig,
                jackson2JsonMessageConverter,
                dataRequestNotifier,
                dataFlowRequestRepository,
                new ClientRegistryClient(builder, clientRegistryProperties));
    }

    @Bean
    public DataFlowRequest dataRequest(WebClient.Builder builder,
                                       DataFlowRequestRepository dataFlowRequestRepository,
                                       PostDataFlowRequestApproval postDataFlowRequestApproval,
                                       DataFlowAuthServerProperties dataFlowAuthServerProperties,
                                       DataFlowConsentManagerProperties dataFlowConsentManagerProperties) {
        return new DataFlowRequest(new ConsentManagerClient(builder, dataFlowAuthServerProperties, dataFlowConsentManagerProperties),
                dataFlowRequestRepository, postDataFlowRequestApproval);
    }

    @Bean
    public DataFlowRequestRepository dataRequestRepository(PgPool pgPool) {
        return new DataFlowRequestRepository(pgPool);
    }

    @Bean
    public DataRequestNotifier dataFlowClient(WebClient.Builder builder) {
        return new DataRequestNotifier(builder);
    }

}
