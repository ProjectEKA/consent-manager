package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.MessageListenerContainerFactory;
import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.ClientRegistryClient;
import in.projecteka.consentmanager.clients.ConsentArtefactNotifier;
import in.projecteka.consentmanager.consent.model.ConsentArtefactsNotificationMessage;
import in.projecteka.consentmanager.consent.model.HIPConsentArtefactRepresentation;
import in.projecteka.consentmanager.consent.model.request.HIUNotificationRequest;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactReference;
import lombok.AllArgsConstructor;
import org.apache.log4j.Logger;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

import static in.projecteka.consentmanager.ConsentManagerConfiguration.CONSENT_GRANTED_QUEUE;
import static in.projecteka.consentmanager.clients.ClientError.queueNotFound;

@AllArgsConstructor
public class ConsentArtefactBroadcastListener {
    private static final Logger logger = Logger.getLogger(ConsentArtefactBroadcastListener.class);
    private MessageListenerContainerFactory messageListenerContainerFactory;
    private DestinationsConfig destinationsConfig;
    private Jackson2JsonMessageConverter converter;
    private ConsentArtefactNotifier consentArtefactNotifier;
    private ClientRegistryClient clientRegistryClient;

    @PostConstruct
    public void subscribe() throws ClientError {
        DestinationsConfig.DestinationInfo destinationInfo = destinationsConfig
                .getQueues()
                .get(CONSENT_GRANTED_QUEUE);
        if (destinationInfo == null) {
            logger.error(CONSENT_GRANTED_QUEUE + " not found");
            throw queueNotFound();
        }

        MessageListenerContainer mlc = messageListenerContainerFactory
                .createMessageListenerContainer(destinationInfo.getRoutingKey());

        MessageListener messageListener = message -> {
            ConsentArtefactsNotificationMessage consentArtefactsNotificationMessage =
                    (ConsentArtefactsNotificationMessage) converter.fromMessage(message);
            logger.info("Received message for Request id : " + consentArtefactsNotificationMessage
                    .getRequestId());

            notifyHiu(consentArtefactsNotificationMessage);
            notifyHips(consentArtefactsNotificationMessage);
        };
        mlc.setupMessageListener(messageListener);

        mlc.start();
    }

    private void notifyHiu(ConsentArtefactsNotificationMessage consentArtefactsNotificationMessage) {
        HIUNotificationRequest hiuNotificationRequest = hiuNotificationRequest(
                consentArtefactsNotificationMessage, consentArtefactsNotificationMessage.getRequestId());
        String hiuCallBackUrl = consentArtefactsNotificationMessage.getHiuCallBackUrl();
        consentArtefactNotifier.notifyHiu(hiuNotificationRequest, hiuCallBackUrl).block();
    }

    private void notifyHips(ConsentArtefactsNotificationMessage consentArtefactsNotificationMessage) {
        consentArtefactsNotificationMessage
                .getConsentArtefacts()
                .forEach(this::sendConsentArtefact);
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

    private HIUNotificationRequest hiuNotificationRequest(
            ConsentArtefactsNotificationMessage consentArtefactsNotificationMessage,
            String requestId) {
        List<ConsentArtefactReference> consentArtefactReferences = consentArtefactsNotificationMessage
                .getConsentArtefacts()
                .stream()
                .map(consentArtefact -> ConsentArtefactReference
                        .builder()
                        .status(consentArtefact.getStatus())
                        .id(consentArtefact.getConsentDetail().getConsentId())
                        .build())
                .collect(Collectors.toList());

        return HIUNotificationRequest
                .builder()
                .consents(consentArtefactReferences)
                .consentRequestId(requestId)
                .build();
    }
}
