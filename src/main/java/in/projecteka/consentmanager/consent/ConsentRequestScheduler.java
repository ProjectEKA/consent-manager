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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
        Optional<List<ConsentRequestDetail>> consentRequestDetails =
                consentRequestRepository.getConsentsByStatus(REQUESTED).collectList().blockOptional();
        consentRequestDetails.orElse(Collections.emptyList()).stream()
                .filter(consentRequestDetail -> isConsentRequestExpired(consentRequestDetail.getCreatedAt()))
                .forEach(consentRequestDetail -> {
                    consentRequestRepository.updateStatus(consentRequestDetail.getRequestId(), EXPIRED).block();
                    broadcastConsentArtefacts(List.of(),
                            consentRequestDetail.getRequestId(),
                            EXPIRED,
                            consentRequestDetail.getLastUpdated(),
                            consentRequestDetail.getHiu().getId()).block();
                    logger.info("Consent request with id {} is expired", consentRequestDetail.getRequestId());
                });
    }

    private boolean isConsentRequestExpired(LocalDateTime createdAt) {
        LocalDateTime requestExpiry = createdAt.plus(Duration.ofMinutes(consentServiceProperties.getConsentRequestExpiry()));
        return requestExpiry.isBefore(LocalDateTime.now(ZoneOffset.UTC));
    }

    private Mono<Void> broadcastConsentArtefacts(List<HIPConsentArtefactRepresentation> consents,
                                                 String requestId,
                                                 ConsentStatus status,
                                                 LocalDateTime lastUpdated,
                                                 String hiuId) {
        ConsentArtefactsMessage message = ConsentArtefactsMessage
                .builder()
                .status(status)
                .timestamp(lastUpdated)
                .consentRequestId(requestId)
                .consentArtefacts(consents)
                .hiuId(hiuId)
                .build();
        return consentNotificationPublisher.publish(message);
    }
}
