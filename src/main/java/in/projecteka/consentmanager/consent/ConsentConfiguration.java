package in.projecteka.consentmanager.consent;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.MessageListenerContainerFactory;
import in.projecteka.consentmanager.clients.ConsentArtefactNotifier;
import in.projecteka.consentmanager.clients.ConsentManagerClient;
import in.projecteka.consentmanager.clients.OtpServiceClient;
import in.projecteka.consentmanager.clients.PatientServiceClient;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.clients.properties.GatewayServiceProperties;
import in.projecteka.consentmanager.clients.properties.LinkServiceProperties;
import in.projecteka.consentmanager.clients.properties.OtpServiceProperties;
import in.projecteka.consentmanager.common.CentralRegistry;
import in.projecteka.consentmanager.common.IdentityService;
import in.projecteka.consentmanager.common.ListenerProperties;
import in.projecteka.consentmanager.common.ServiceAuthentication;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import in.projecteka.consentmanager.user.UserServiceProperties;
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
    public ConsentNotificationPublisher postConsentApproval(AmqpTemplate amqpTemplate,
                                                            DestinationsConfig destinationsConfig) {
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
                                                ServiceAuthentication serviceAuthentication,
                                                CentralRegistry centralRegistry,
                                                PostConsentRequest postConsentRequest,
                                                LinkServiceProperties linkServiceProperties,
                                                IdentityService identityService,
                                                ConceptValidator conceptValidator,
                                                GatewayServiceProperties gatewayServiceProperties) {
        return new ConsentManager(
                new UserServiceClient(builder, userServiceProperties.getUrl(),
                        identityService::authenticate,
                        gatewayServiceProperties,
                        serviceAuthentication),
                repository,
                consentArtefactRepository,
                keyPair,
                consentNotificationPublisher,
                centralRegistry,
                postConsentRequest,
                new PatientServiceClient(builder, identityService::authenticate, linkServiceProperties.getUrl()),
                new CMProperties(gatewayServiceProperties.getClientId()),
                conceptValidator,
                new ConsentArtefactQueryGenerator(),
                new ConsentManagerClient(builder,
                        gatewayServiceProperties.getBaseUrl(),
                        identityService::authenticate,
                        gatewayServiceProperties,
                        serviceAuthentication));
    }

    @Bean
    public ConsentScheduler consentScheduler(
            ConsentRequestRepository repository,
            ConsentArtefactRepository consentArtefactRepository,
            ConsentNotificationPublisher consentNotificationPublisher) {
        return new ConsentScheduler(repository, consentArtefactRepository, consentNotificationPublisher);
    }

    @Bean
    ConsentRequestScheduler consentRequestScheduler(ConsentRequestRepository repository,
                                                    ConsentServiceProperties consentServiceProperties,
                                                    ConsentNotificationPublisher consentNotificationPublisher) {
        return new ConsentRequestScheduler(repository, consentServiceProperties, consentNotificationPublisher);
    }

    @Bean
    public Jackson2JsonMessageConverter converter() {
        var objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public MessageListenerContainerFactory messageListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter) {
        return new MessageListenerContainerFactory(connectionFactory, jackson2JsonMessageConverter);
    }

    @Bean
    public ConsentArtefactNotifier consentArtefactClient(WebClient.Builder builder,
                                                         ServiceAuthentication serviceAuthentication,
                                                         GatewayServiceProperties gatewayServiceProperties) {
        return new ConsentArtefactNotifier(builder, serviceAuthentication::authenticate, gatewayServiceProperties);
    }

    @Bean
    public HiuConsentNotificationListener hiuNotificationListener(
            MessageListenerContainerFactory messageListenerContainerFactory,
            DestinationsConfig destinationsConfig,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter,
            ConsentArtefactNotifier consentArtefactNotifier,
            AmqpTemplate amqpTemplate,
            ListenerProperties listenerProperties) {
        return new HiuConsentNotificationListener(
                messageListenerContainerFactory,
                destinationsConfig,
                jackson2JsonMessageConverter,
                consentArtefactNotifier,
                amqpTemplate,
                listenerProperties);
    }

    @Bean
    public HipConsentNotificationListener hipNotificationListener(
            MessageListenerContainerFactory messageListenerContainerFactory,
            DestinationsConfig destinationsConfig,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter,
            ConsentArtefactNotifier consentArtefactNotifier,
            ConsentArtefactRepository consentArtefactRepository) {
        return new HipConsentNotificationListener(
                messageListenerContainerFactory,
                destinationsConfig,
                jackson2JsonMessageConverter,
                consentArtefactNotifier,
                consentArtefactRepository);
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
            IdentityService identityService,
            GatewayServiceProperties gatewayServiceProperties,
            ServiceAuthentication serviceAuthentication) {
        return new ConsentRequestNotificationListener(
                messageListenerContainerFactory,
                destinationsConfig,
                jackson2JsonMessageConverter,
                new OtpServiceClient(builder, otpServiceProperties.getUrl()),
                new UserServiceClient(builder,
                        userServiceProperties.getUrl(),
                        identityService::authenticate,
                        gatewayServiceProperties,
                        serviceAuthentication),
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
                                                                   CacheAdapter<String, String> usedTokens) {
        return new PinVerificationTokenService(key, usedTokens);
    }
}
