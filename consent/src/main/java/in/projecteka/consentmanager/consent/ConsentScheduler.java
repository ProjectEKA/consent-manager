package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.consent.model.ConsentArtefact;
import in.projecteka.consentmanager.consent.model.ConsentArtefactsMessage;
import in.projecteka.consentmanager.consent.model.ConsentRepresentation;
import in.projecteka.consentmanager.consent.model.HIPConsentArtefact;
import in.projecteka.consentmanager.consent.model.HIPConsentArtefactRepresentation;
import in.projecteka.consentmanager.consent.model.HIPReference;
import in.projecteka.consentmanager.consent.model.HIUReference;
import in.projecteka.library.clients.model.ClientError;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static in.projecteka.consentmanager.consent.model.ConsentStatus.EXPIRED;
import static in.projecteka.consentmanager.consent.model.ConsentStatus.GRANTED;

@AllArgsConstructor
public class ConsentScheduler {
    private static final Logger logger = LoggerFactory.getLogger(ConsentScheduler.class);

    private final ConsentArtefactRepository consentArtefactRepository;
    private final ConsentNotificationPublisher consentNotificationPublisher;

    @Scheduled(cron = "${consentmanager.scheduler.consentExpiryCronExpr}")
    @Async
    public void processExpiredConsents() {
        logger.info("Processing consent for expiry");
        var consentExpiries = consentArtefactRepository.getConsentArtefacts(GRANTED).collectList().block();
        if (consentExpiries != null) {
            consentExpiries.forEach(consentExpiry -> {
                if (isConsentExpired(consentExpiry.getConsentExpiryDate())) {
                    ConsentRepresentation consentRepresentation = getConsentRepresentation(
                            consentExpiry.getConsentId(),
                            consentExpiry.getPatientId()).block();
                    if (consentRepresentation != null) {
                        updateAndBroadcastConsentExpiry(consentExpiry.getConsentId(), consentRepresentation);
                    }
                    logger.info("Consent with id {} is expired", consentExpiry.getConsentId());
                }
            });
        }
    }

    private boolean isConsentExpired(LocalDateTime dateExpiryAt) {
        return dateExpiryAt.isBefore(LocalDateTime.now(ZoneOffset.UTC));
    }

    private Mono<ConsentRepresentation> getConsentRepresentation(String consentId, String requesterId) {
        return getConsentWithRequest(consentId)
                .filter(consentRepresentation -> !isNotSameRequester(consentRepresentation.getConsentDetail(),
                        requesterId))
                .switchIfEmpty(Mono.error(ClientError.consentArtefactForbidden()))
                .filter(this::isGrantedConsent)
                .switchIfEmpty(Mono.error(ClientError.consentNotGranted()));
    }

    private Mono<ConsentRepresentation> getConsentWithRequest(String consentId) {
        return consentArtefactRepository.getConsentWithRequest(consentId)
                .switchIfEmpty(Mono.error(ClientError.consentArtefactNotFound()));
    }

    private static boolean isNotSameRequester(ConsentArtefact consentDetail, String requesterId) {
        return !consentDetail.getHiu().getId().equals(requesterId) &&
                !consentDetail.getPatient().getId().equals(requesterId);
    }

    private boolean isGrantedConsent(ConsentRepresentation consentRepresentation) {
        return consentRepresentation.getStatus().equals(GRANTED);
    }

    private void updateAndBroadcastConsentExpiry(String consentId, ConsentRepresentation consentRepresentation) {
        consentArtefactRepository.updateConsentArtefactStatus(consentId, EXPIRED).block();
        broadcastConsentArtefacts(
                consentRepresentation.getConsentRequestId(),
                consentRepresentation.getDateModified(),
                consentId,
                consentRepresentation.getConsentDetail().getHip(),
                consentRepresentation.getConsentDetail().getCreatedAt(),
                consentRepresentation.getConsentDetail().getHiu()).block();

    }

    private Mono<Void> broadcastConsentArtefacts(String requestId,
                                                 LocalDateTime lastUpdated,
                                                 String consentId,
                                                 HIPReference hip,
                                                 LocalDateTime createdAt,
                                                 HIUReference hiuReference) {
        HIPConsentArtefactRepresentation hipConsentArtefactRepresentation = HIPConsentArtefactRepresentation
                .builder()
                .status(EXPIRED)
                .consentId(consentId)
                .consentDetail(HIPConsentArtefact
                        .builder()
                        .hip(hip)
                        .consentId(consentId)
                        .createdAt(createdAt)
                        .build())
                .build();

        ConsentArtefactsMessage message = ConsentArtefactsMessage
                .builder()
                .status(EXPIRED)
                .timestamp(lastUpdated)
                .consentRequestId(requestId)
                .consentArtefacts(List.of(hipConsentArtefactRepresentation))
                .hiuId(hiuReference.getId())
                .build();
        return consentNotificationPublisher.publish(message);
    }
}
