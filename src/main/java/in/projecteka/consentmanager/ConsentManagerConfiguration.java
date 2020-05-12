package in.projecteka.consentmanager;

import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.consentmanager.clients.ClientRegistryClient;
import in.projecteka.consentmanager.clients.IdentityServiceClient;
import in.projecteka.consentmanager.clients.properties.ClientRegistryProperties;
import in.projecteka.consentmanager.clients.properties.IdentityServiceProperties;
import in.projecteka.consentmanager.common.CentralRegistry;
import in.projecteka.consentmanager.common.CentralRegistryTokenVerifier;
import in.projecteka.consentmanager.common.IdentityService;
import in.projecteka.consentmanager.common.ListenerProperties;
import in.projecteka.consentmanager.link.ClientErrorExceptionHandler;
import in.projecteka.consentmanager.user.LockedUsersRepository;
import in.projecteka.consentmanager.user.TokenService;
import in.projecteka.consentmanager.user.UserRepository;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.net.URL;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.text.ParseException;
import java.util.HashMap;

@Configuration
public class ConsentManagerConfiguration {
    public static final String HIU_CONSENT_NOTIFICATION_QUEUE = "hiu-consent-notification-queue";
    public static final String HIP_CONSENT_NOTIFICATION_QUEUE = "hip-consent-notification-queue";
    public static final String HIP_DATA_FLOW_REQUEST_QUEUE = "hip-data-flow-request-queue";
    public static final String CONSENT_REQUEST_QUEUE = "consent-request-queue";
    public static final String DEAD_LETTER_QUEUE = "cm-dead-letter-queue";
    private static final String CM_DEAD_LETTER_EXCHANGE = "cm-dead-letter-exchange";
    public static final String PARKING_EXCHANGE = "parking.exchange";
    public static final String PARKING_QUEUE = "parking.queue";

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
    public LockedUsersRepository lockedUsersRepository(DbOptions dbOptions) {
        return new LockedUsersRepository(pgPool(dbOptions));
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
    public DestinationsConfig destinationsConfig(AmqpAdmin amqpAdmin, ListenerProperties listenerProperties) {
        HashMap<String, DestinationsConfig.DestinationInfo> queues = new HashMap<>();
        queues.put(CONSENT_REQUEST_QUEUE, new DestinationsConfig.DestinationInfo("exchange", CONSENT_REQUEST_QUEUE));
        queues.put(HIU_CONSENT_NOTIFICATION_QUEUE,
                new DestinationsConfig.DestinationInfo("exchange", HIU_CONSENT_NOTIFICATION_QUEUE));
        queues.put(HIP_CONSENT_NOTIFICATION_QUEUE,
                new DestinationsConfig.DestinationInfo("exchange", HIP_CONSENT_NOTIFICATION_QUEUE));
        queues.put(HIP_DATA_FLOW_REQUEST_QUEUE,
                new DestinationsConfig.DestinationInfo("exchange", HIP_DATA_FLOW_REQUEST_QUEUE));

        Queue parkingLotQueue = QueueBuilder.durable(PARKING_QUEUE).build();
        Binding parkingBinding = BindingBuilder
                .bind(parkingLotQueue)
                .to(new TopicExchange(PARKING_EXCHANGE))
                .with("#");
        amqpAdmin.declareQueue(parkingLotQueue);
        amqpAdmin.declareExchange(new TopicExchange(PARKING_EXCHANGE));
        amqpAdmin.declareBinding(parkingBinding);

        Queue deadLetterQueue = QueueBuilder.durable(DEAD_LETTER_QUEUE).deadLetterExchange("exchange").ttl(listenerProperties.getRetryInterval()).build();
        Binding with = BindingBuilder
                .bind(deadLetterQueue)
                .to(new TopicExchange(CM_DEAD_LETTER_EXCHANGE))
                .with("#");
        amqpAdmin.declareQueue(deadLetterQueue);
        amqpAdmin.declareExchange(new TopicExchange(CM_DEAD_LETTER_EXCHANGE));
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
    public IdentityService identityService(IdentityServiceClient identityServiceClient,
                                           IdentityServiceProperties identityServiceProperties) {
        return new IdentityService(identityServiceClient, identityServiceProperties);
    }

    @Bean
    public TokenService tokenService(IdentityServiceProperties identityServiceProperties,
                                     IdentityServiceClient identityServiceClient, UserRepository userRepository) {
        return new TokenService(identityServiceProperties, identityServiceClient, userRepository);
    }

    @Bean("pinSigning")
    public KeyPair keyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(2048);
        return keyPairGen.genKeyPair();
    }

    @Bean("keySigningPublicKey")
    public PublicKey publicKey(@Qualifier("pinSigning") KeyPair keyPair) {
        return keyPair.getPublic();
    }

    @Bean("keySigningPrivateKey")
    public PrivateKey privateKey(@Qualifier("pinSigning") KeyPair keyPair) {
        return keyPair.getPrivate();
    }

    @Bean("centralRegistryJWKSet")
    public JWKSet jwkSet(ClientRegistryProperties clientRegistryProperties) throws IOException, ParseException {
        return JWKSet.load(new URL(clientRegistryProperties.getJwkUrl()));
    }

    @Bean("identityServiceJWKSet")
    public JWKSet identityServiceJWKSet(IdentityServiceProperties identityServiceProperties)
            throws IOException, ParseException {
        return JWKSet.load(new URL(identityServiceProperties.getJwkUrl()));
    }

    @Bean
    public CentralRegistryTokenVerifier centralRegistryTokenVerifier(@Qualifier("centralRegistryJWKSet") JWKSet jwkSet) {
        return new CentralRegistryTokenVerifier(jwkSet);
    }
}
