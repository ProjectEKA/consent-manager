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
public class DataFlowRequest {
    private final ConsentManagerClient consentManagerClient;
    private DataFlowRequestRepository dataFlowRequestRepository;
    private PostDataFlowRequestApproval postDataFlowrequestApproval;

    public Mono<DataFlowRequestResponse> validateDataTransferRequest(
            String hiuId,
            in.projecteka.consentmanager.dataflow.model.DataFlowRequest dataFlowRequest) {
        final String transactionId = UUID.randomUUID().toString();
        return fetchConsentArtefact(dataFlowRequest.getConsent().getId())
                .flatMap(consentArtefactRepresentation -> isValidHIU(hiuId, consentArtefactRepresentation)
                        ? validateConsentExpiry(dataFlowRequest, transactionId, consentArtefactRepresentation)
                        : Mono.error(ClientError.invalidHIU()));
    }

    private Mono<DataFlowRequestResponse> validateConsentExpiry(
            in.projecteka.consentmanager.dataflow.model.DataFlowRequest dataFlowRequest,
            String transactionId,
            ConsentArtefactRepresentation consentArtefactRepresentation) {
        return isConsentExpired(consentArtefactRepresentation)
                ? Mono.error(ClientError.consentExpired())
                : validateIfHIDateRangeIsProvided(dataFlowRequest, transactionId, consentArtefactRepresentation);
    }

    private Mono<ConsentArtefactRepresentation> fetchConsentArtefact(String consentArtefactId) {
        return consentManagerClient.getConsentArtefact(consentArtefactId);
    }

    private Mono<DataFlowRequestResponse> validateIfHIDateRangeIsProvided(
            in.projecteka.consentmanager.dataflow.model.DataFlowRequest dataFlowRequest,
            String transactionId,
            ConsentArtefactRepresentation consentArtefactRepresentation) {
        if (dataFlowRequest.getHiDataRange() == null) {
            return saveAndBroadcast(
                    addDefaultHIDataRange(dataFlowRequest, consentArtefactRepresentation),
                    transactionId,
                    consentArtefactRepresentation.getSignature());
        }
        return validateHIDateRange(dataFlowRequest, transactionId, consentArtefactRepresentation);
    }

    private Mono<DataFlowRequestResponse> validateHIDateRange(
            in.projecteka.consentmanager.dataflow.model.DataFlowRequest dataFlowRequest,
            String transactionId,
            ConsentArtefactRepresentation consentArtefactRepresentation) {
        boolean validHIDateRange = isValidHIDateRange(dataFlowRequest, consentArtefactRepresentation);
        return validHIDateRange
                ? saveAndBroadcast(dataFlowRequest, transactionId, consentArtefactRepresentation.getSignature())
                : Mono.error(ClientError.invalidDateRange());
    }

    private Mono<DataFlowRequestResponse> saveAndBroadcast(
            in.projecteka.consentmanager.dataflow.model.DataFlowRequest dataFlowRequest,
            String transactionId,
            String signature) {
        setConsentSignature(dataFlowRequest, signature);
        return dataFlowRequestRepository.addDataFlowRequest(transactionId, dataFlowRequest)
                .then(postDataFlowrequestApproval.broadcastDataFlowRequest(transactionId, dataFlowRequest))
                .thenReturn(DataFlowRequestResponse.builder().transactionId(transactionId).build());
    }

    private void setConsentSignature(in.projecteka.consentmanager.dataflow.model.DataFlowRequest dataFlowRequest, String signature) {
        dataFlowRequest.getConsent().setDigitalSignature(signature);
    }

    private in.projecteka.consentmanager.dataflow.model.DataFlowRequest addDefaultHIDataRange(
            in.projecteka.consentmanager.dataflow.model.DataFlowRequest dataFlowRequest,
            ConsentArtefactRepresentation consentArtefactRepresentation) {
        if (dataFlowRequest.getHiDataRange() == null)
            dataFlowRequest.setHiDataRange(
                    HIDataRange.builder()
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
                            .build());
        return dataFlowRequest;
    }

    private boolean isValidHIDateRange(in.projecteka.consentmanager.dataflow.model.DataFlowRequest dataFlowRequest,
                                       ConsentArtefactRepresentation consentArtefactRepresentation) {
        return dataFlowRequest.getHiDataRange().getFrom()
                .after(consentArtefactRepresentation.getConsentDetail().getPermission().getDateRange().getFromDate()) &&
                dataFlowRequest.getHiDataRange().getTo()
                        .before(consentArtefactRepresentation.getConsentDetail().getPermission().getDateRange().getToDate()) &&
                dataFlowRequest.getHiDataRange().getFrom().before(dataFlowRequest.getHiDataRange().getTo());
    }

    private boolean isConsentExpired(ConsentArtefactRepresentation consentArtefactRepresentation) {
        return consentArtefactRepresentation.getConsentDetail().getPermission().getDataExpiryAt().before(new Date());
    }

    private boolean isValidHIU(String hiuId, ConsentArtefactRepresentation consentArtefactRepresentation) {
        return consentArtefactRepresentation.getConsentDetail().getHiu().getId().equals(hiuId);
    }
}
