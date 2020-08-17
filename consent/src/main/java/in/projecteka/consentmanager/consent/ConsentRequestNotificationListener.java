package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.MessageListenerContainerFactory;
import in.projecteka.consentmanager.clients.PatientServiceClient;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.consent.model.ConsentRequest;
import in.projecteka.consentmanager.consent.model.Content;
import in.projecteka.consentmanager.consent.model.GrantedContext;
import in.projecteka.consentmanager.consent.model.HIType;
import in.projecteka.consentmanager.consent.model.request.GrantedConsent;
import in.projecteka.consentmanager.consent.policies.NhsPolicyCheck;
import in.projecteka.library.clients.OtpServiceClient;
import in.projecteka.library.clients.model.Action;
import in.projecteka.library.clients.model.Communication;
import in.projecteka.library.clients.model.CommunicationType;
import in.projecteka.library.clients.model.Notification;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static in.projecteka.consentmanager.Constants.CONSENT_REQUEST_QUEUE;

@AllArgsConstructor
public class ConsentRequestNotificationListener {
    private static final Logger logger = LoggerFactory.getLogger(ConsentRequestNotificationListener.class);
    private final MessageListenerContainerFactory messageListenerContainerFactory;
    private final Jackson2JsonMessageConverter converter;
    private final OtpServiceClient consentNotificationClient;
    private final UserServiceClient userServiceClient;
    private final ConsentServiceProperties consentServiceProperties;
    private final ConsentManager consentManager;
    private final PatientServiceClient patientServiceClient;
    private final NHSProperties nhsProperties;

    @PostConstruct
    public void subscribe() {
        var mlc = messageListenerContainerFactory.createMessageListenerContainer(CONSENT_REQUEST_QUEUE);
        MessageListener messageListener = message -> {
            try {
                ConsentRequest consentRequest = (ConsentRequest) converter.fromMessage(message);
                logger.info("Received message for Request id : {}", consentRequest.getId());
                processConsentRequest(consentRequest);
            } catch (Exception e) {
                throw new AmqpRejectAndDontRequeueException(e.getMessage(), e);
            }
        };
        mlc.setupMessageListener(messageListener);
        mlc.start();
    }

    public Mono<Void> notifyUserWith(Notification<Content> notification) {
        return consentNotificationClient.send(notification);
    }

    private void processConsentRequest(ConsentRequest consentRequest) {
        try {
            if (isAutoApproveConsentRequest(consentRequest)) {
                autoApproveFor(consentRequest).subscribe();
                return;
            }
            createNotificationMessage(consentRequest).flatMap(this::notifyUserWith).block();
        } catch (Exception exception) {
            logger.error(exception.getMessage());
        }
    }

    private boolean isAutoApproveConsentRequest(ConsentRequest consentRequest) {
        return new NhsPolicyCheck().checkPolicyFor(consentRequest, nhsProperties.getHiuId());
    }

    private Mono<Notification<Content>> createNotificationMessage(ConsentRequest consentRequest) {
        return userServiceClient.userOf(consentRequest.getDetail().getPatient().getId())
                .map(user -> new Notification<>(consentRequest.getId().toString(),
                        Communication.builder()
                                .communicationType(CommunicationType.MOBILE)
                                .value(user.getPhone())
                                .build(),
                        Content.builder()
                                .requester(consentRequest.getDetail().getRequester().getName())
                                .consentRequestId(consentRequest.getId())
                                .hiTypes(Arrays.stream(consentRequest.getDetail().getHiTypes())
                                        .map(HIType::getValue)
                                        .collect(Collectors.joining(",")))
                                .deepLinkUrl(String.format("%s/consent/%s",
                                        consentServiceProperties.getUrl(),
                                        consentRequest.getId()))
                                .build(),
                        Action.CONSENT_REQUEST_CREATED));
    }

    private Mono<Void> autoApproveFor(ConsentRequest consentRequest) {
        List<GrantedContext> grantedContexts = new ArrayList<>();
        List<GrantedConsent> grantedConsents = new ArrayList<>();
        return patientServiceClient.retrievePatientLinks(consentRequest.getDetail().getPatient().getId())
                .map(linkedCareContexts -> linkedCareContexts.getCareContext(consentRequest.getDetail().getHip().getId()))
                .flatMap(linkedCareContexts -> {
                    linkedCareContexts.forEach(careContext -> {
                        grantedContexts.add(GrantedContext.builder()
                                .patientReference(careContext.getPatientRefNo())
                                .careContextReference(careContext.getCareContextRefNo())
                                .build());
                    });

                    grantedConsents.add(GrantedConsent.builder()
                            .careContexts(grantedContexts)
                            .hip(consentRequest.getDetail().getHip())
                            .hiTypes(consentRequest.getDetail().getHiTypes())
                            .permission(consentRequest.getDetail().getPermission())
                            .build());
                    return consentManager.approveConsent(consentRequest.getDetail().getPatient().getId(),
                            consentRequest.getId().toString(),
                            grantedConsents);
                }).then();
    }
}
