package in.projecteka.consentmanager;

import in.projecteka.consentmanager.clients.ClientRegistryClient;
import in.projecteka.consentmanager.clients.properties.ClientRegistryProperties;
import in.projecteka.consentmanager.common.CentralRegistry;
import in.projecteka.consentmanager.link.ClientErrorExceptionHandler;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;

@Configuration
public class ConsentManagerConfiguration {
    public static final String HIU_CONSENT_NOTIFICATION_QUEUE = "hiu-consent-notification-queue";
    public static final String HIP_CONSENT_NOTIFICATION_QUEUE = "hip-consent-notification-queue";
    public static final String HIP_DATA_FLOW_REQUEST_QUEUE = "hip-data-flow-request-queue";
    public static final String CONSENT_REQUEST_QUEUE = "consent-request-queue";
    public static final String DEAD_LETTER_QUEUE = "cm-dead-letter-queue";
    private static final String CM_DEAD_LETTER_EXCHANGE = "cm-dead-letter-exchange";
    private static final String CM_DEAD_LETTER_ROUTING_KEY = "cm-dead-letter";

    @Bean
    public CentralRegistry centralRegistry(ClientRegistryClient clientRegistryClient,
                                           ClientRegistryProperties clientRegistryProperties) {
        return new CentralRegistry(clientRegistryClient, clientRegistryProperties);
    }

    @Bean
    public ClientRegistryClient clientRegistryClient(WebClient.Builder builder,
                                                     ClientRegistryProperties clientRegistryProperties) {
        return new ClientRegistryClient(builder, clientRegistryProperties.getUrl());
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
    public DestinationsConfig destinationsConfig(AmqpAdmin amqpAdmin) {
        HashMap<String, DestinationsConfig.DestinationInfo> queues = new HashMap<>();
        queues.put(CONSENT_REQUEST_QUEUE, new DestinationsConfig.DestinationInfo("exchange", CONSENT_REQUEST_QUEUE));
        queues.put(HIU_CONSENT_NOTIFICATION_QUEUE,
                new DestinationsConfig.DestinationInfo("exchange", HIU_CONSENT_NOTIFICATION_QUEUE));
        queues.put(HIP_CONSENT_NOTIFICATION_QUEUE,
                new DestinationsConfig.DestinationInfo("exchange", HIP_CONSENT_NOTIFICATION_QUEUE));
        queues.put(HIP_DATA_FLOW_REQUEST_QUEUE,
                new DestinationsConfig.DestinationInfo("exchange", HIP_DATA_FLOW_REQUEST_QUEUE));

        Queue deadLetterQueue = QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
        Binding with = BindingBuilder
                .bind(deadLetterQueue)
                .to(new DirectExchange(CM_DEAD_LETTER_EXCHANGE))
                .with(CM_DEAD_LETTER_ROUTING_KEY);
        amqpAdmin.declareQueue(deadLetterQueue);
        amqpAdmin.declareExchange(new DirectExchange(CM_DEAD_LETTER_EXCHANGE));
        amqpAdmin.declareBinding(with);

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
                            .deadLetterExchange(CM_DEAD_LETTER_EXCHANGE)
                            .deadLetterRoutingKey(CM_DEAD_LETTER_ROUTING_KEY)
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
}
