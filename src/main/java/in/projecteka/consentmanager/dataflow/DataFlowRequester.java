package in.projecteka.consentmanager.dataflow;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.ConsentManagerClient;
import in.projecteka.consentmanager.dataflow.model.ConsentArtefactRepresentation;
import in.projecteka.consentmanager.dataflow.model.DataFlowRequestResponse;
import in.projecteka.consentmanager.dataflow.model.HIDataRange;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.UUID;

@AllArgsConstructor
public class DataFlowRequester {
    private final ConsentManagerClient consentManagerClient;
    private DataFlowRequestRepository dataFlowRequestRepository;
    private PostDataFlowRequestApproval postDataFlowrequestApproval;

    public Mono<DataFlowRequestResponse> requestHealthData(
            String hiuId,
            in.projecteka.consentmanager.dataflow.model.DataFlowRequest dataFlowRequest) {
        final String transactionId = UUID.randomUUID().toString();
        return fetchConsentArtefact(dataFlowRequest.getConsent().getId())
                .flatMap(caRep -> validateAndSaveConsent(transactionId, dataFlowRequest, caRep, hiuId))
                .then(notifyHIP(transactionId, dataFlowRequest))
                .thenReturn(DataFlowRequestResponse.builder().transactionId(transactionId).build());
    }

    private Mono<Void> notifyHIP(String transactionId,
                                 in.projecteka.consentmanager.dataflow.model.DataFlowRequest dataFlowRequest) {
         return postDataFlowrequestApproval.broadcastDataFlowRequest(transactionId, dataFlowRequest);
    }

    private Mono<Void> validateAndSaveConsent(String transactionId,
                                              in.projecteka.consentmanager.dataflow.model.DataFlowRequest dataFlowRequest,
                                              ConsentArtefactRepresentation consentArtefactRepresentation,
                                              String hiuId) {
        if (!isValidHIU(hiuId, consentArtefactRepresentation)) {
            return Mono.error(ClientError.invalidHIU());
        }
        if (isConsentExpired(consentArtefactRepresentation)) {
            return Mono.error(ClientError.consentExpired());
        }
        if (dataFlowRequest.getHiDataRange() == null) {
            dataFlowRequest.setHiDataRange(defaultDateRange(consentArtefactRepresentation));
        } else if (!isValidHIDateRange(dataFlowRequest, consentArtefactRepresentation)) {
            return Mono.error(ClientError.invalidDateRange());
        }
        dataFlowRequest.setArtefactSignature(consentArtefactRepresentation.getSignature());
        return dataFlowRequestRepository.addDataFlowRequest(transactionId, dataFlowRequest);
    }

    private HIDataRange defaultDateRange(ConsentArtefactRepresentation consentArtefactRepresentation) {
        return HIDataRange.builder()
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

    private boolean isValidHIDateRange(in.projecteka.consentmanager.dataflow.model.DataFlowRequest dataFlowRequest,
                                       ConsentArtefactRepresentation consentArtefactRepresentation) {
        return ((dataFlowRequest.getHiDataRange().getFrom()
                .equals(consentArtefactRepresentation.getConsentDetail().getPermission().getDateRange().getFromDate())) ||
                (dataFlowRequest.getHiDataRange().getFrom()
                .after(consentArtefactRepresentation.getConsentDetail().getPermission().getDateRange().getFromDate()))) &&
                ((dataFlowRequest.getHiDataRange().getTo()
                        .equals(consentArtefactRepresentation.getConsentDetail().getPermission().getDateRange().getToDate())) ||
                (dataFlowRequest.getHiDataRange().getTo()
                        .before(consentArtefactRepresentation.getConsentDetail().getPermission().getDateRange().getToDate()))) &&
                dataFlowRequest.getHiDataRange().getFrom().before(dataFlowRequest.getHiDataRange().getTo());
    }

    private boolean isConsentExpired(ConsentArtefactRepresentation consentArtefactRepresentation) {
        return consentArtefactRepresentation.getConsentDetail().getPermission().getDataExpiryAt().before(new Date());
    }

    private boolean isValidHIU(String hiuId, ConsentArtefactRepresentation consentArtefactRepresentation) {
        return consentArtefactRepresentation.getConsentDetail().getHiu().getId().equals(hiuId);
    }
}
