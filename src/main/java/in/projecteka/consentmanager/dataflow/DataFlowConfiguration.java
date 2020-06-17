package in.projecteka.consentmanager.dataflow;

import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.MessageListenerContainerFactory;
import in.projecteka.consentmanager.clients.ConsentManagerClient;
import in.projecteka.consentmanager.clients.DataRequestNotifier;
import in.projecteka.consentmanager.clients.properties.GatewayServiceProperties;
import in.projecteka.consentmanager.common.CentralRegistry;
import in.projecteka.consentmanager.common.IdentityService;
import io.vertx.pgclient.PgPool;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class DataFlowConfiguration {

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
                                                               CentralRegistry centralRegistry) {
        return new DataFlowBroadcastListener(messageListenerContainerFactory,
                destinationsConfig,
                jackson2JsonMessageConverter,
                dataRequestNotifier,
                dataFlowRequestRepository,
                centralRegistry);
    }

    @Bean
    public DataFlowRequester dataRequest(WebClient.Builder builder,
                                         DataFlowRequestRepository dataFlowRequestRepository,
                                         PostDataFlowRequestApproval postDataFlowRequestApproval,
                                         DataFlowConsentManagerProperties dataFlowConsentManagerProperties,
                                         IdentityService identityService,
                                         GatewayServiceProperties gatewayServiceProperties,
                                         CentralRegistry centralRegistry) {
        return new DataFlowRequester(
                new ConsentManagerClient(builder,
                        dataFlowConsentManagerProperties.getUrl(),
                        identityService::authenticate,
                        gatewayServiceProperties,
                        centralRegistry),
                dataFlowRequestRepository,
                postDataFlowRequestApproval);
    }

    @Bean
    public DataFlowRequestRepository dataRequestRepository(PgPool pgPool) {
        return new DataFlowRequestRepository(pgPool);
    }

    @Bean
    public DataRequestNotifier dataFlowClient(WebClient.Builder builder, CentralRegistry centralRegistry) {
        return new DataRequestNotifier(builder, centralRegistry::authenticate);
    }
}
