package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.MessageListenerContainerFactory;
import in.projecteka.consentmanager.clients.ClientRegistryClient;
import in.projecteka.consentmanager.clients.ConsentArtefactNotifier;
import in.projecteka.consentmanager.clients.ConsentNotificationClient;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.clients.properties.ClientRegistryProperties;
import in.projecteka.consentmanager.clients.properties.OtpServiceProperties;
import in.projecteka.consentmanager.clients.properties.UserServiceProperties;
import in.projecteka.consentmanager.consent.repository.ConsentArtefactRepository;
import in.projecteka.consentmanager.consent.repository.ConsentRequestRepository;
import io.vertx.pgclient.PgPool;
import lombok.SneakyThrows;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

@Configuration
public class ConsentConfiguration {

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
    public PostConsentRequestNotification postConsentRequestNotification(AmqpTemplate amqpTemplate,
                                                                         DestinationsConfig destinationsConfig) {
        return new PostConsentRequestNotification(amqpTemplate, destinationsConfig);
    }

    @Bean
    public ConsentManager consentRequestService(WebClient.Builder builder,
                                                ConsentRequestRepository repository,
                                                ClientRegistryProperties clientRegistryProperties,
                                                UserServiceProperties userServiceProperties,
                                                ConsentArtefactRepository consentArtefactRepository,
                                                KeyPair keyPair,
                                                PostConsentApproval postConsentApproval,
                                                PostConsentRequestNotification postConsentRequestNotification) {
        return new ConsentManager(
                new ClientRegistryClient(builder, clientRegistryProperties),
                new UserServiceClient(builder, userServiceProperties),
                repository,
                consentArtefactRepository,
                keyPair,
                postConsentApproval,
                postConsentRequestNotification);
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
                                                                    ConsentArtefactNotifier consentArtefactNotifier,
                                                                    ClientRegistryClient clientRegistryClient) {
        return new ConsentArtefactBroadcastListener(
                messageListenerContainerFactory,
                destinationsConfig,
                jackson2JsonMessageConverter,
                consentArtefactNotifier,
                clientRegistryClient);
    }

    @Bean
    public ConsentRequestNotificationListener consentRequestNotificationListener(
                                                                    MessageListenerContainerFactory messageListenerContainerFactory,
                                                                    DestinationsConfig destinationsConfig,
                                                                    Jackson2JsonMessageConverter jackson2JsonMessageConverter,
                                                                    WebClient.Builder builder,
                                                                    OtpServiceProperties otpServiceProperties,
                                                                    UserServiceProperties userServiceProperties,
                                                                    ConsentServiceProperties consentServiceProperties
                                                                    ) {
        return new ConsentRequestNotificationListener(
                messageListenerContainerFactory,
                destinationsConfig,
                jackson2JsonMessageConverter,
                new ConsentNotificationClient(otpServiceProperties, builder),
                new UserServiceClient(builder, userServiceProperties),
                consentServiceProperties
        );
    }

    @SneakyThrows
    @Bean
    public KeyPair keyPair() {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }
}
