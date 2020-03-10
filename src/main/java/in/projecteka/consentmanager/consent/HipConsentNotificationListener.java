package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.MessageListenerContainerFactory;
import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.ClientRegistryClient;
import in.projecteka.consentmanager.clients.ConsentArtefactNotifier;
import in.projecteka.consentmanager.consent.model.HIPConsentArtefactRepresentation;
import lombok.AllArgsConstructor;
import org.apache.log4j.Logger;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;

import static in.projecteka.consentmanager.ConsentManagerConfiguration.HIP_CONSENT_NOTIFICATION_QUEUE;
import static in.projecteka.consentmanager.clients.ClientError.queueNotFound;

@AllArgsConstructor
public class HipConsentNotificationListener {
    private static final Logger logger = Logger.getLogger(HiuConsentNotificationListener.class);
    private MessageListenerContainerFactory messageListenerContainerFactory;
    private DestinationsConfig destinationsConfig;
    private Jackson2JsonMessageConverter converter;
    private ConsentArtefactNotifier consentArtefactNotifier;
    private ClientRegistryClient clientRegistryClient;

    @PostConstruct
    public void subscribe() throws ClientError {
        DestinationsConfig.DestinationInfo destinationInfo = destinationsConfig
                .getQueues()
                .get(HIP_CONSENT_NOTIFICATION_QUEUE);
        if (destinationInfo == null) {
            logger.error(HIP_CONSENT_NOTIFICATION_QUEUE + " not found");
            throw queueNotFound();
        }

        MessageListenerContainer mlc = messageListenerContainerFactory
                .createMessageListenerContainer(destinationInfo.getRoutingKey());

        MessageListener messageListener = message -> {
            try {
                HIPConsentArtefactRepresentation consentArtefact =
                        (HIPConsentArtefactRepresentation) converter.fromMessage(message);
                logger.info(String.format("Received notify consent to hip for consent artefact: %s",
                        consentArtefact.getConsentDetail().getConsentId()));

                sendConsentArtefact(consentArtefact);
            } catch (Exception e) {
                logger.error(e);
                throw new AmqpRejectAndDontRequeueException(e);
            }
        };
        mlc.setupMessageListener(messageListener);

        mlc.start();
    }

    private void sendConsentArtefact(HIPConsentArtefactRepresentation consentArtefact) {
        String hipId = consentArtefact.getConsentDetail().getHip().getId();
        getProviderUrl(hipId)
                .flatMap(providerUrl -> sendArtefactTo(consentArtefact, providerUrl))
                .block();
    }

    private Mono<Void> sendArtefactTo(HIPConsentArtefactRepresentation consentArtefact, String providerUrl) {
        return consentArtefactNotifier.sendConsentArtefactTo(consentArtefact, providerUrl);
    }

    private Mono<String> getProviderUrl(String hipId) {
        return clientRegistryClient.providerWith(hipId)
                .flatMap(provider -> Mono.just(provider.getProviderUrl()));
    }

}
