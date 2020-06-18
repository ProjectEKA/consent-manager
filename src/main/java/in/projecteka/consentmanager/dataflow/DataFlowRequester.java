package in.projecteka.consentmanager.dataflow;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.ConsentManagerClient;
import in.projecteka.consentmanager.dataflow.model.ConsentArtefactRepresentation;
import in.projecteka.consentmanager.dataflow.model.ConsentStatus;
import in.projecteka.consentmanager.dataflow.model.DataFlowRequest;
import in.projecteka.consentmanager.dataflow.model.DataFlowRequestResponse;
import in.projecteka.consentmanager.dataflow.model.DateRange;
import in.projecteka.consentmanager.dataflow.model.HealthInfoNotificationRequest;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@AllArgsConstructor
public class DataFlowRequester {
    private final ConsentManagerClient consentManagerClient;
    private final DataFlowRequestRepository dataFlowRequestRepository;
    private final PostDataFlowRequestApproval postDataFlowrequestApproval;

    public Mono<DataFlowRequestResponse> requestHealthData(DataFlowRequest dataFlowRequest) {
        final String transactionId = UUID.randomUUID().toString();
        return fetchConsentArtefact(dataFlowRequest.getConsent().getId())
                .flatMap(caRep -> saveNotificationRequest(dataFlowRequest, caRep))
                .flatMap(flowRequest -> dataFlowRequestRepository.addDataFlowRequest(transactionId, flowRequest)
                        .thenReturn(flowRequest))
                .flatMap(flowRequest -> notifyHIP(transactionId, flowRequest))
                .thenReturn(DataFlowRequestResponse.builder().transactionId(transactionId).build());
    }

    private Mono<Void> notifyHIP(String transactionId, DataFlowRequest dataFlowRequest) {
        return postDataFlowrequestApproval.broadcastDataFlowRequest(transactionId, dataFlowRequest);
    }

    private Mono<DataFlowRequest> saveNotificationRequest(
            DataFlowRequest dataFlowRequest,
            ConsentArtefactRepresentation consentArtefactRepresentation) {

        if (isConsentExpired(consentArtefactRepresentation)) {
            return Mono.error(ClientError.consentExpired());
        }
        if (!isConsentGranted(consentArtefactRepresentation)) {
            return Mono.error(ClientError.consentNotGranted());
        }
        if (dataFlowRequest.getDateRange() != null &&
                !isValidHIDateRange(dataFlowRequest, consentArtefactRepresentation)) {
            return Mono.error(ClientError.invalidDateRange());
        }
        var flowRequestBuilder = dataFlowRequest.toBuilder();
        if (dataFlowRequest.getDateRange() == null) {
            flowRequestBuilder.dateRange(defaultDateRange(consentArtefactRepresentation));
        }
        var consent = dataFlowRequest.getConsent().toBuilder().build();
        return Mono.just(flowRequestBuilder.consent(consent).build());
    }

    private boolean isConsentGranted(ConsentArtefactRepresentation consentArtefactRepresentation) {
        return consentArtefactRepresentation.getStatus().equals(ConsentStatus.GRANTED);
    }

    private DateRange defaultDateRange(ConsentArtefactRepresentation consentArtefactRepresentation) {
        return DateRange.builder()
                .from(consentArtefactRepresentation
                        .getConsentDetail()
                        .getPermission()
                        .getDateRange()
                        .getFromDate())
                .to(consentArtefactRepresentation
                        .getConsentDetail()
                        .getPermission()
                        .getDateRange()
                        .getToDate())
                .build();
    }

    private Mono<ConsentArtefactRepresentation> fetchConsentArtefact(String consentArtefactId) {
        return consentManagerClient.getConsentArtefact(consentArtefactId);
    }

    private boolean isValidHIDateRange(DataFlowRequest dataFlowRequest,
                                       ConsentArtefactRepresentation consentArtefactRepresentation) {
        boolean equalOrAfter = isEqualOrAfter(
                dataFlowRequest.getDateRange().getFrom(),
                consentArtefactRepresentation.fromDate());
        boolean equalOrBefore = isEqualOrBefore(
                dataFlowRequest.getDateRange().getTo(),
                consentArtefactRepresentation.toDate());
        boolean before = dataFlowRequest.getDateRange().getFrom().isBefore(dataFlowRequest.getDateRange().getTo());
        return equalOrAfter && equalOrBefore && before;
    }

    private boolean isEqualOrBefore(LocalDateTime requestDate,
                                    LocalDateTime permissionDate) {
        return requestDate.equals(permissionDate) || requestDate.isBefore(permissionDate);
    }

    private boolean isEqualOrAfter(LocalDateTime requestDate, LocalDateTime permissionDate) {
        return requestDate.equals(permissionDate) || requestDate.isAfter(permissionDate);
    }

    private boolean isConsentExpired(ConsentArtefactRepresentation consentArtefactRepresentation) {
        return consentArtefactRepresentation.getConsentDetail().getPermission().getDataEraseAt().isBefore(LocalDateTime.now());
    }

    private boolean isValidHIU(String hiuId, ConsentArtefactRepresentation consentArtefactRepresentation) {
        return consentArtefactRepresentation.getConsentDetail().getHiu().getId().equals(hiuId);
    }

    @Deprecated
    public Mono<Void> notifyHealthInfoStatus(String requesterId, HealthInfoNotificationRequest notificationRequest) {
        return Mono.just(notificationRequest.getRequestId())
                .filterWhen(this::validateRequest)
                .switchIfEmpty(Mono.error(ClientError.requestAlreadyExists()))
                .flatMap(val -> (!validateRequester(requesterId, notificationRequest))
                        ? Mono.error(ClientError.invalidRequester())
                        : dataFlowRequestRepository.saveNotificationRequest(notificationRequest));
    }

    private Mono<Boolean> validateRequest(UUID requestId) {
        return dataFlowRequestRepository.getIfPresent(requestId)
                .map(Objects::isNull)
                .switchIfEmpty(Mono.just(true));
    }

    private boolean validateRequester(String requesterId, HealthInfoNotificationRequest notificationRequest) {
        return notificationRequest.getNotifier().getId().equals(requesterId);
    }

    public Mono<Void> notifyHealthInformationStatus(HealthInformationNotificationRequest notificationRequest) {
        return Mono.just(notificationRequest.getRequestId())
                .filterWhen(this::validateRequest)
                .switchIfEmpty(Mono.error(ClientError.requestAlreadyExists()))
                .flatMap(val -> dataFlowRequestRepository.saveHealthNotificationRequest(notificationRequest));
    }
}
