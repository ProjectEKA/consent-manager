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
import in.projecteka.consentmanager.properties.GatewayServiceProperties;
import in.projecteka.consentmanager.properties.KeyPairConfig;
import in.projecteka.consentmanager.properties.LinkServiceProperties;
import in.projecteka.consentmanager.properties.ListenerProperties;
import in.projecteka.library.common.CentralRegistry;
import in.projecteka.library.common.IdentityService;
import in.projecteka.library.common.ServiceAuthentication;
import in.projecteka.library.common.cache.CacheAdapter;
import io.vertx.pgclient.PgPool;
import lombok.SneakyThrows;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.KeyPair;
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
    public PatientServiceClient patientServiceClient(
            @Qualifier("customBuilder") WebClient.Builder builder,
            @Value("${consentmanager.authorization.header}") String authorizationHeader,
            IdentityService identityService,
            LinkServiceProperties linkServiceProperties) {
        return new PatientServiceClient(builder.build(),
                identityService::authenticate,
                linkServiceProperties.getUrl(),
                authorizationHeader);
    }

    @Bean
    public ConsentManager consentManager(
            UserServiceClient userServiceClient,
            ConsentServiceProperties consentServiceProperties,
            ConsentRequestRepository repository,
            ConsentArtefactRepository consentArtefactRepository,
            KeyPair keyPair,
            ConsentNotificationPublisher consentNotificationPublisher,
            CentralRegistry centralRegistry,
            PostConsentRequest postConsentRequest,
            ConceptValidator conceptValidator,
            GatewayServiceProperties gatewayServiceProperties,
            PatientServiceClient patientServiceClient,
            ConsentManagerClient consentManagerClient) {
        return new ConsentManager(userServiceClient,
                consentServiceProperties,
                repository,
                consentArtefactRepository,
                keyPair,
                consentNotificationPublisher,
                centralRegistry,
                postConsentRequest,
                patientServiceClient,
                new CMProperties(gatewayServiceProperties.getClientId()),
                conceptValidator,
                new ConsentArtefactQueryGenerator(),
                consentManagerClient);
    }

    @Bean
    public ConsentManagerClient consentManagerClient(@Qualifier("customBuilder") WebClient.Builder builder,
                                                     ServiceAuthentication serviceAuthentication,
                                                     IdentityService identityService,
                                                     GatewayServiceProperties gatewayServiceProperties) {
        return new ConsentManagerClient(builder,
                gatewayServiceProperties.getBaseUrl(),
                identityService::authenticate,
                gatewayServiceProperties,
                serviceAuthentication);
    }

    @Bean
    @ConditionalOnProperty(prefix = "consentmanager.scheduler",
            value = "consent-artefact-expiry-enabled",
            havingValue = "true")
    public ConsentScheduler consentScheduler(
            ConsentArtefactRepository consentArtefactRepository,
            ConsentNotificationPublisher consentNotificationPublisher) {
        return new ConsentScheduler(consentArtefactRepository, consentNotificationPublisher);
    }

    @Bean
    @ConditionalOnProperty(prefix = "consentmanager.scheduler",
            value = "consent-request-expiry-enabled",
            havingValue = "true")
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
    public ConsentArtefactNotifier consentArtefactClient(@Qualifier("customBuilder") WebClient.Builder builder,
                                                         ServiceAuthentication serviceAuthentication,
                                                         GatewayServiceProperties gatewayServiceProperties) {
        return new ConsentArtefactNotifier(builder, serviceAuthentication::authenticate, gatewayServiceProperties);
    }

    @Bean
    public HiuConsentNotificationListener hiuNotificationListener(
            MessageListenerContainerFactory messageListenerContainerFactory,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter,
            ConsentArtefactNotifier consentArtefactNotifier,
            AmqpTemplate amqpTemplate,
            ListenerProperties listenerProperties) {
        return new HiuConsentNotificationListener(
                messageListenerContainerFactory,
                jackson2JsonMessageConverter,
                consentArtefactNotifier,
                amqpTemplate,
                listenerProperties);
    }

    @Bean
    public HipConsentNotificationListener hipNotificationListener(
            MessageListenerContainerFactory messageListenerContainerFactory,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter,
            ConsentArtefactNotifier consentArtefactNotifier,
            ConsentArtefactRepository consentArtefactRepository) {
        return new HipConsentNotificationListener(
                messageListenerContainerFactory,
                jackson2JsonMessageConverter,
                consentArtefactNotifier,
                consentArtefactRepository);
    }

    @Bean
    public ConsentRequestNotificationListener consentRequestNotificationListener(
            MessageListenerContainerFactory messageListenerContainerFactory,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter,
            OtpServiceClient otpServiceClient,
            ConsentServiceProperties consentServiceProperties,
            ConsentManager consentManager,
            NHSProperties nhsProperties,
            UserServiceClient userServiceClient,
            PatientServiceClient patientServiceClient) {
        return new ConsentRequestNotificationListener(
                messageListenerContainerFactory,
                jackson2JsonMessageConverter,
                otpServiceClient,
                userServiceClient,
                consentServiceProperties,
                consentManager,
                patientServiceClient,
                nhsProperties);
    }

    @SneakyThrows
    @Bean
    public KeyPair keyPair(KeyPairConfig keyPairConfig) {
        return keyPairConfig.createSignArtefactKeyPair();
    }

    @Bean
    public PinVerificationTokenService pinVerificationTokenService(@Qualifier("keySigningPublicKey") PublicKey key,
                                                                   CacheAdapter<String, String> usedTokens) {
        return new PinVerificationTokenService(key, usedTokens);
    }
}