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
        return fetchConsentArtifact(dataFlowRequest.getConsent().getId())
                .flatMap(consentArtefactRepresentation -> isValidHIU(hiuId, consentArtefactRepresentation)
                        .flatMap(validHiu -> validHiu ? isConsentExpired(consentArtefactRepresentation)
                                .flatMap(consentExpired -> consentExpired ?
                                        Mono.error(ClientError.consentExpired()) :
                                        validateHIDateRange(dataFlowRequest, transactionId,
                                                consentArtefactRepresentation)) :
                                Mono.error(ClientError.invalidHIU())));
    }

    private Mono<ConsentArtefactRepresentation> fetchConsentArtifact(String consentArtefactId) {
        return consentManagerClient.getConsentArtifact(consentArtefactId);
    }

    private Mono<DataFlowRequestResponse> validateHIDateRange(
            in.projecteka.consentmanager.dataflow.model.DataFlowRequest dataFlowRequest,
            String transactionId, ConsentArtefactRepresentation consentArtefactRepresentation) {
        return dataFlowRequest.getHiDataRange() == null ?
                addDefaultHIDataRange(dataFlowRequest, consentArtefactRepresentation)
                        .flatMap(request ->
                                saveAndBroadcast(request, transactionId)) :
                isValidHIDateRange(dataFlowRequest, consentArtefactRepresentation)
                        .flatMap(validHIDateRange -> validHIDateRange ?
                                saveAndBroadcast(dataFlowRequest, transactionId) :
                                Mono.error(ClientError.invalidDateRange())
                        );
    }

    private Mono<DataFlowRequestResponse> saveAndBroadcast(
            in.projecteka.consentmanager.dataflow.model.DataFlowRequest dataFlowRequest,
            String transactionId) {
        return getDigitalSignatureForHIP(dataFlowRequest.getConsent().getId())
                .flatMap(signature -> {
                    dataFlowRequest.getConsent().setDigitalSignature(signature);
                    return dataFlowRequestRepository.addDataFlowRequest(transactionId, dataFlowRequest)
                            .thenReturn(postDataFlowrequestApproval.broadcastDataFlowRequest(transactionId,
                                    dataFlowRequest))
                            .thenReturn(DataFlowRequestResponse.builder().transactionId(transactionId).build());
                });
    }

    private Mono<String> getDigitalSignatureForHIP(String consentArtifactId) {
        //TODO: Need to create a hash out of consent without HIU info
        return Mono.just("new hash from consent artifact");
    }

    private Mono<in.projecteka.consentmanager.dataflow.model.DataFlowRequest> addDefaultHIDataRange(
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
        return Mono.just(dataFlowRequest);
    }

    private Mono<Boolean> isValidHIDateRange(
            in.projecteka.consentmanager.dataflow.model.DataFlowRequest dataFlowRequest,
            ConsentArtefactRepresentation consentArtefactRepresentation) {
        return Mono.just(dataFlowRequest.getHiDataRange().getFrom()
                .after(consentArtefactRepresentation.getConsentDetail().getPermission().getDateRange().getFromDate()) &&
                dataFlowRequest.getHiDataRange().getTo()
                        .before(consentArtefactRepresentation.getConsentDetail().getPermission().getDateRange().getToDate()) &&
                dataFlowRequest.getHiDataRange().getFrom().before(dataFlowRequest.getHiDataRange().getTo()));
    }

    private Mono<Boolean> isConsentExpired(ConsentArtefactRepresentation consentArtefactRepresentation) {
        return Mono.just(consentArtefactRepresentation.getConsentDetail().getPermission().getDataExpiryAt().before(new Date()));
    }

    private Mono<Boolean> isValidHIU(String hiuId, ConsentArtefactRepresentation consentArtefactRepresentation) {
        return Mono.just(consentArtefactRepresentation.getConsentDetail().getHiu().getId().equals(hiuId));
    }


}
