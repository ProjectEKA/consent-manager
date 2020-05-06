package in.projecteka.consentmanager.link.link;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.LinkServiceClient;
import in.projecteka.consentmanager.clients.model.Identifier;
import in.projecteka.consentmanager.clients.model.Patient;
import in.projecteka.consentmanager.clients.model.PatientLinkReferenceResponse;
import in.projecteka.consentmanager.clients.model.PatientLinkRequest;
import in.projecteka.consentmanager.clients.model.PatientLinkResponse;
import in.projecteka.consentmanager.common.CentralRegistry;
import in.projecteka.consentmanager.link.link.model.PatientLinkReferenceRequest;
import in.projecteka.consentmanager.link.link.model.PatientLinksResponse;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

import static in.projecteka.consentmanager.link.link.Transformer.toHIPPatient;

@AllArgsConstructor
public class Link {
    private final LinkServiceClient linkServiceClient;
    private final LinkRepository linkRepository;
    private final CentralRegistry centralRegistry;

    public Mono<PatientLinkReferenceResponse> patientWith(String patientId,
                                                          PatientLinkReferenceRequest patientLinkReferenceRequest) {
        Patient patient = toHIPPatient(patientId, patientLinkReferenceRequest.getPatient());
        var linkReferenceRequest = new in.projecteka.consentmanager.clients.model.PatientLinkReferenceRequest(
                patientLinkReferenceRequest.getRequestId().toString(),
                patientLinkReferenceRequest.getTransactionId(),
                patient);
        return Mono.just(patientLinkReferenceRequest.getRequestId())
                .filterWhen(this::validateRequest)
                .switchIfEmpty(Mono.error(ClientError.requestAlreadyExists()))
                .flatMap(id -> linkRepository.getHIPIdFromDiscovery(patientLinkReferenceRequest.getTransactionId())
                        .flatMap(hipId -> providerUrl(hipId)
                                .switchIfEmpty(Mono.error(ClientError.unableToConnectToProvider()))
                                .flatMap(url -> getPatientLinkReferenceResponse(patientLinkReferenceRequest,
                                        linkReferenceRequest,
                                        hipId,
                                        url))));
    }


    private Mono<Boolean> validateRequest(UUID requestId) {
        return linkRepository.selectLinkReference(requestId)
                .map(Objects::isNull)
                .switchIfEmpty(Mono.just(true));
    }

    private Mono<PatientLinkReferenceResponse> getPatientLinkReferenceResponse(
            PatientLinkReferenceRequest patientLinkReferenceRequest,
            in.projecteka.consentmanager.clients.model.PatientLinkReferenceRequest linkReferenceRequest,
            String hipId,
            String url) {
        return centralRegistry
                .authenticate()
                .flatMap(token -> linkServiceClient.linkPatientEnquiry(linkReferenceRequest, url, token))
                .flatMap(linkReferenceResponse -> {
                    linkReferenceResponse.setTransactionId(patientLinkReferenceRequest.getTransactionId());
                    return linkRepository.insertToLinkReference(linkReferenceResponse,
                            hipId,
                            patientLinkReferenceRequest.getRequestId())
                            .thenReturn(PatientLinkReferenceResponse.builder()
                                    .transactionId(linkReferenceResponse.getTransactionId())
                                    .link(linkReferenceResponse.getLink()).build());
                });
    }

    public Mono<PatientLinkResponse> verifyToken(String linkRefNumber,
                                                 PatientLinkRequest patientLinkRequest,
                                                 String patientId) {
        return linkCareContexts(patientLinkRequest, linkRefNumber, patientId);
    }

    private Mono<PatientLinkResponse> linkCareContexts(PatientLinkRequest patientLinkRequest,
                                                       String linkRefNumber,
                                                       String patientId) {
        return linkRepository.getTransactionIdFromLinkReference(linkRefNumber)
                .flatMap(linkRepository::getHIPIdFromDiscovery)
                .flatMap(hipId -> providerUrl(hipId)
                        .switchIfEmpty(Mono.error(ClientError.unableToConnectToProvider()))
                        .flatMap(url -> getPatientLinkResponse(patientLinkRequest,
                                linkRefNumber,
                                patientId,
                                hipId,
                                url)));
    }

    private Mono<PatientLinkResponse> getPatientLinkResponse(
            PatientLinkRequest patientLinkRequest,
            String linkRefNumber,
            String patientId,
            String hipId,
            String url) {
        return centralRegistry.authenticate()
                .flatMap(token ->
                        linkServiceClient.linkPatientConfirmation(linkRefNumber, patientLinkRequest, url, token))
                .flatMap(patientLinkResponse ->
                        linkRepository.insertToLink(hipId, patientId, linkRefNumber, patientLinkResponse.getPatient())
                                .thenReturn(PatientLinkResponse.builder()
                                        .patient(patientLinkResponse.getPatient())
                                        .build()));
    }

    private Mono<String> providerUrl(String providerId) {
        return centralRegistry.providerWith(providerId)
                .flatMap(provider -> provider.getIdentifiers()
                        .stream()
                        .filter(Identifier::isOfficial)
                        .findFirst()
                        .map(identifier -> Mono.just(identifier.getSystem()))
                        .orElse(Mono.empty()));
    }

    public Mono<PatientLinksResponse> getLinkedCareContexts(String patientId) {
        return linkRepository.getLinkedCareContextsForAllHip(patientId).map(patientLinks ->
                PatientLinksResponse.builder().patient(patientLinks).build());
    }
}
