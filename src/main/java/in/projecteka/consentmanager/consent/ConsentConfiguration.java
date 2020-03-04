package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.MessageListenerContainerFactory;
import in.projecteka.consentmanager.clients.ClientRegistryClient;
import in.projecteka.consentmanager.clients.ConsentArtefactNotifier;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.clients.properties.ClientRegistryProperties;
import in.projecteka.consentmanager.clients.properties.UserServiceProperties;
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
    public HiuConsentNotificationListener hiuNotificationListener(MessageListenerContainerFactory messageListenerContainerFactory,
                                                                  DestinationsConfig destinationsConfig,
                                                                  Jackson2JsonMessageConverter jackson2JsonMessageConverter,
                                                                  ConsentArtefactNotifier consentArtefactNotifier) {
        return new HiuConsentNotificationListener(
                messageListenerContainerFactory,
                destinationsConfig,
                jackson2JsonMessageConverter,
                consentArtefactNotifier);
    }

    @Bean
    public HipConsentNotificationListener hipNotificationListener(MessageListenerContainerFactory messageListenerContainerFactory,
                                                                  DestinationsConfig destinationsConfig,
                                                                  Jackson2JsonMessageConverter jackson2JsonMessageConverter,
                                                                  ConsentArtefactNotifier consentArtefactNotifier,
                                                                  ClientRegistryClient clientRegistryClient) {
        return new HipConsentNotificationListener(
                messageListenerContainerFactory,
                destinationsConfig,
                jackson2JsonMessageConverter,
                consentArtefactNotifier,
                clientRegistryClient);
    }

    @SneakyThrows
    @Bean
    public KeyPair keyPair() {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }
}
