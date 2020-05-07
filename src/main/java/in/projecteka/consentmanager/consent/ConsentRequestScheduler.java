package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.consent.model.ConsentArtefactsMessage;
import in.projecteka.consentmanager.consent.model.ConsentRequestDetail;
import in.projecteka.consentmanager.consent.model.ConsentStatus;
import in.projecteka.consentmanager.consent.model.HIPConsentArtefactRepresentation;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static in.projecteka.consentmanager.consent.model.ConsentStatus.EXPIRED;
import static in.projecteka.consentmanager.consent.model.ConsentStatus.REQUESTED;

@AllArgsConstructor
public class ConsentRequestScheduler {
    private static final Logger logger = LoggerFactory.getLogger(ConsentRequestScheduler.class);

    private final ConsentRequestRepository consentRequestRepository;
    private final ConsentServiceProperties consentServiceProperties;
    private final ConsentNotificationPublisher consentNotificationPublisher;

    @Scheduled(cron = "${consentmanager.scheduler.consentRequestExpiryCronExpr}")
    @Async
    public void processExpiredConsentRequests() {
        logger.info("Processing consent requests for expiry");
        List<ConsentRequestDetail> consentRequestDetails = consentRequestRepository.getConsentsByStatus(REQUESTED).
                collectList().block();
        if (consentRequestDetails != null) {
            consentRequestDetails.forEach(consentRequestDetail -> {
                if (isConsentRequestExpired(consentRequestDetail.getCreatedAt())) {
                    consentRequestRepository.updateStatus(consentRequestDetail.getRequestId(), EXPIRED).block();
                    broadcastConsentArtefacts(List.of(),
                            consentRequestDetail.getConsentNotificationUrl(),
                            consentRequestDetail.getRequestId(),
                            EXPIRED,
                            consentRequestDetail.getLastUpdated()).block();
                    logger.info("Consent request with id {} is expired", consentRequestDetail.getRequestId());
                }
            });
        }
    }

    private boolean isConsentRequestExpired(Date createdAt) {
        Instant requestExpiry =
                createdAt.toInstant().plus(Duration.ofMinutes(consentServiceProperties.getConsentRequestExpiry()));
        return requestExpiry.isBefore(new Date().toInstant());
    }

    private Mono<Void> broadcastConsentArtefacts(List<HIPConsentArtefactRepresentation> consents,
                                                 String hiuConsentNotificationUrl,
                                                 String requestId,
                                                 ConsentStatus status,
                                                 Date lastUpdated) {
        ConsentArtefactsMessage message = ConsentArtefactsMessage
                .builder()
                .status(status)
                .timestamp(lastUpdated)
                .consentRequestId(requestId)
                .consentArtefacts(consents)
                .hiuConsentNotificationUrl(hiuConsentNotificationUrl)
                .build();
        return consentNotificationPublisher.publish(message);
    }
}
