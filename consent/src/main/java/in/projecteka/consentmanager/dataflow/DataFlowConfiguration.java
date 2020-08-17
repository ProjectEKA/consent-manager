package in.projecteka.consentmanager.dataflow;

import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.MessageListenerContainerFactory;
import in.projecteka.consentmanager.clients.ConsentManagerClient;
import in.projecteka.consentmanager.clients.DataFlowRequestClient;
import in.projecteka.consentmanager.clients.DataRequestNotifier;
import in.projecteka.consentmanager.properties.GatewayServiceProperties;
import in.projecteka.library.common.IdentityService;
import in.projecteka.library.common.ServiceAuthentication;
import io.vertx.pgclient.PgPool;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
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
    public DataFlowBroadcastListener dataFlowBroadcastListener(
            MessageListenerContainerFactory messageListenerContainerFactory,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter,
            DataRequestNotifier dataRequestNotifier,
            DataFlowRequestRepository dataFlowRequestRepository) {
        return new DataFlowBroadcastListener(messageListenerContainerFactory,
                jackson2JsonMessageConverter,
                dataRequestNotifier,
                dataFlowRequestRepository);
    }

    @Bean
    public DataFlowRequestClient dataFlowRequestClient(@Qualifier("customBuilder") WebClient.Builder builder,
                                                       GatewayServiceProperties gatewayServiceProperties,
                                                       ServiceAuthentication serviceAuthentication) {
        return new DataFlowRequestClient(builder, gatewayServiceProperties, serviceAuthentication);
    }

    @Bean
    public DataFlowRequester dataRequest(@Qualifier("customBuilder") WebClient.Builder builder,
                                         DataFlowRequestRepository dataFlowRequestRepository,
                                         PostDataFlowRequestApproval postDataFlowRequestApproval,
                                         DataFlowConsentManagerProperties dataFlowConsentManagerProperties,
                                         IdentityService identityService,
                                         GatewayServiceProperties gatewayServiceProperties,
                                         ServiceAuthentication serviceAuthentication,
                                         DataFlowRequestClient dataFlowRequestClient) {
        return new DataFlowRequester(
                new ConsentManagerClient(builder,
                        dataFlowConsentManagerProperties.getUrl(),
                        identityService::authenticate,
                        gatewayServiceProperties,
                        serviceAuthentication),
                dataFlowRequestRepository,
                postDataFlowRequestApproval,
                dataFlowRequestClient);
    }

    @Bean
    public DataFlowRequestRepository dataRequestRepository(PgPool pgPool) {
        return new DataFlowRequestRepository(pgPool);
    }

    @Bean
    public DataRequestNotifier dataFlowClient(@Qualifier("customBuilder") WebClient.Builder builder,
                                              ServiceAuthentication serviceAuthentication,
                                              GatewayServiceProperties gatewayServiceProperties) {
        return new DataRequestNotifier(builder.build(), serviceAuthentication::authenticate, gatewayServiceProperties);
    }
}
