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

import java.util.Date;
import java.util.UUID;

@AllArgsConstructor
public class DataFlowRequester {
    private final ConsentManagerClient consentManagerClient;
    private final DataFlowRequestRepository dataFlowRequestRepository;
    private final PostDataFlowRequestApproval postDataFlowrequestApproval;

    public Mono<DataFlowRequestResponse> requestHealthData(String hiuId, DataFlowRequest dataFlowRequest) {
        final String transactionId = UUID.randomUUID().toString();
        return fetchConsentArtefact(dataFlowRequest.getConsent().getId())
                .flatMap(caRep -> validate(dataFlowRequest, caRep, hiuId))
                .flatMap(flowRequest -> dataFlowRequestRepository.addDataFlowRequest(transactionId, flowRequest)
                        .thenReturn(flowRequest))
                .flatMap(flowRequest -> notifyHIP(transactionId, flowRequest))
                .thenReturn(DataFlowRequestResponse.builder().transactionId(transactionId).build());
    }

    private Mono<Void> notifyHIP(String transactionId, DataFlowRequest dataFlowRequest) {
        return postDataFlowrequestApproval.broadcastDataFlowRequest(transactionId, dataFlowRequest);
    }

    private Mono<DataFlowRequest> validate(
            DataFlowRequest dataFlowRequest,
            ConsentArtefactRepresentation consentArtefactRepresentation,
            String hiuId) {
        if (!isValidHIU(hiuId, consentArtefactRepresentation)) {
            return Mono.error(ClientError.invalidRequester());
        }
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
        var consent = dataFlowRequest
                .getConsent()
                .toBuilder()
                .digitalSignature(consentArtefactRepresentation.getSignature())
                .build();
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
        return isEqualOrAfter(dataFlowRequest.getDateRange().getFrom(), consentArtefactRepresentation.fromDate()) &&
                isEqualOrBefore(dataFlowRequest.getDateRange().getTo(), consentArtefactRepresentation.toDate()) &&
                dataFlowRequest.getDateRange().getFrom().before(dataFlowRequest.getDateRange().getTo());
    }

    private boolean isEqualOrBefore(Date requestDate,
                                    Date permissionDate) {
        return requestDate.equals(permissionDate) || requestDate.before(permissionDate);
    }

    private boolean isEqualOrAfter(Date requestDate, Date permissionDate) {
        return requestDate.equals(permissionDate) || requestDate.after(permissionDate);
    }

    private boolean isConsentExpired(ConsentArtefactRepresentation consentArtefactRepresentation) {
        return consentArtefactRepresentation.getConsentDetail().getPermission().getDataEraseAt().before(new Date());
    }

    private boolean isValidHIU(String hiuId, ConsentArtefactRepresentation consentArtefactRepresentation) {
        return consentArtefactRepresentation.getConsentDetail().getHiu().getId().equals(hiuId);
    }

    public Mono<Void> notifyHealthInfoStatus(String requesterId, HealthInfoNotificationRequest notificationRequest) {
        if (!validateRequester(requesterId, notificationRequest)) {
            return Mono.error(ClientError.invalidRequester());
        }
        return dataFlowRequestRepository.saveNotificationRequest(notificationRequest);
    }

    private boolean validateRequester(String requesterId, HealthInfoNotificationRequest notificationRequest) {
        return notificationRequest.getNotifier().getId().equals(requesterId);
    }
}
