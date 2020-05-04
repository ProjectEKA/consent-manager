package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.MessageListenerContainerFactory;
import in.projecteka.consentmanager.clients.ConsentArtefactNotifier;
import in.projecteka.consentmanager.clients.OtpServiceClient;
import in.projecteka.consentmanager.clients.PatientServiceClient;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.clients.properties.LinkServiceProperties;
import in.projecteka.consentmanager.clients.properties.OtpServiceProperties;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import in.projecteka.consentmanager.user.UserServiceProperties;
import in.projecteka.consentmanager.common.CentralRegistry;
import in.projecteka.consentmanager.common.IdentityService;
import io.vertx.pgclient.PgPool;
import lombok.SneakyThrows;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;

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
    public ConsentNotificationPublisher postConsentApproval(AmqpTemplate amqpTemplate, DestinationsConfig destinationsConfig) {
        return new ConsentNotificationPublisher(amqpTemplate, destinationsConfig);
    }

    @Bean
    public PostConsentRequest postConsentRequestNotification(AmqpTemplate amqpTemplate,
                                                             DestinationsConfig destinationsConfig) {
        return new PostConsentRequest(amqpTemplate, destinationsConfig);
    }

    @Bean
    public ConsentManager consentRequestService(WebClient.Builder builder,
                                                ConsentRequestRepository repository,
                                                UserServiceProperties userServiceProperties,
                                                ConsentArtefactRepository consentArtefactRepository,
                                                KeyPair keyPair,
                                                ConsentNotificationPublisher consentNotificationPublisher,
                                                CentralRegistry centralRegistry,
                                                PostConsentRequest postConsentRequest,
                                                LinkServiceProperties linkServiceProperties,
                                                IdentityService identityService) {
        return new ConsentManager(
                new UserServiceClient(builder, userServiceProperties.getUrl(), identityService::authenticate),
                repository,
                consentArtefactRepository,
                keyPair,
                consentNotificationPublisher,
                centralRegistry,
                postConsentRequest,
                new PatientServiceClient(builder, identityService::authenticate, linkServiceProperties.getUrl()),
                new CMProperties(identityService.getConsentManagerId()),
                new ConsentArtefactQueryGenerator());
    }

    @Bean
    public Jackson2JsonMessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public MessageListenerContainerFactory messageListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter) {
        return new MessageListenerContainerFactory(connectionFactory, jackson2JsonMessageConverter);
    }

    @Bean
    public ConsentArtefactNotifier consentArtefactClient(WebClient.Builder builder, CentralRegistry centralRegistry) {
        return new ConsentArtefactNotifier(builder, centralRegistry::authenticate);
    }

    @Bean
    public HiuConsentNotificationListener hiuNotificationListener(
            MessageListenerContainerFactory messageListenerContainerFactory,
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
    public HipConsentNotificationListener hipNotificationListener(
            MessageListenerContainerFactory messageListenerContainerFactory,
            DestinationsConfig destinationsConfig,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter,
            ConsentArtefactNotifier consentArtefactNotifier,
            CentralRegistry centralRegistry) {
        return new HipConsentNotificationListener(
                messageListenerContainerFactory,
                destinationsConfig,
                jackson2JsonMessageConverter,
                consentArtefactNotifier,
                centralRegistry);
    }

    @Bean
    public ConsentRequestNotificationListener consentRequestNotificationListener(
            MessageListenerContainerFactory messageListenerContainerFactory,
            DestinationsConfig destinationsConfig,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter,
            WebClient.Builder builder,
            OtpServiceProperties otpServiceProperties,
            UserServiceProperties userServiceProperties,
            ConsentServiceProperties consentServiceProperties,
            IdentityService identityService) {
        return new ConsentRequestNotificationListener(
                messageListenerContainerFactory,
                destinationsConfig,
                jackson2JsonMessageConverter,
                new OtpServiceClient(builder, otpServiceProperties.getUrl()),
                new UserServiceClient(builder, userServiceProperties.getUrl(), identityService::authenticate),
                consentServiceProperties);
    }

    @SneakyThrows
    @Bean
    public KeyPair keyPair() {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }

    @Bean
    public PinVerificationTokenService pinVerificationTokenService(@Qualifier("keySigningPublicKey") PublicKey key,
                                                                   CacheAdapter<String,String> usedTokens) {
        return new PinVerificationTokenService(key, usedTokens);
    }
}
