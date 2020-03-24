package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.PatientServiceClient;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.clients.model.Error;
import in.projecteka.consentmanager.clients.model.ErrorCode;
import in.projecteka.consentmanager.clients.model.ErrorRepresentation;
import in.projecteka.consentmanager.common.CentralRegistry;
import in.projecteka.consentmanager.consent.model.*;
import in.projecteka.consentmanager.consent.model.request.GrantedConsent;
import in.projecteka.consentmanager.consent.model.request.RequestedDetail;
import in.projecteka.consentmanager.consent.model.response.ConsentApprovalResponse;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactLight;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactLightRepresentation;
import in.projecteka.consentmanager.consent.model.response.ConsentReference;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactRepresentation;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.Serializable;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignedObject;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@AllArgsConstructor
public class ConsentManager {
    public static final String SHA_1_WITH_RSA = "SHA1withRSA";
    private final UserServiceClient userServiceClient;
    private final ConsentRequestRepository consentRequestRepository;
    private final ConsentArtefactRepository consentArtefactRepository;
    private final KeyPair keyPair;
    private final ConsentNotificationPublisher consentNotificationPublisher;
    private final CentralRegistry centralRegistry;
    private final PostConsentRequest postConsentRequest;
    private final PatientServiceClient patientServiceClient;

    private static boolean isNotSameRequester(ConsentArtefact consentDetail, String requesterId) {
        return !consentDetail.getHiu().getId().equals(requesterId) &&
                !consentDetail.getPatient().getId().equals(requesterId);
    }

    public Mono<String> askForConsent(RequestedDetail requestedDetail) {
        final String requestId = UUID.randomUUID().toString();
        return validatePatient(requestedDetail.getPatient().getId())
                .then(validateHIPAndHIU(requestedDetail))
                .then(saveRequest(requestedDetail, requestId))
                .then(postConsentRequest.broadcastConsentRequestNotification(ConsentRequest.builder()
                        .detail(requestedDetail)
                        .id(requestId)
                        .build()))
                .thenReturn(requestId);
    }

    private Mono<Boolean> validatePatient(String patientId) {
        return userServiceClient.userOf(patientId)
                .onErrorResume(ClientError.class,
                        clientError -> Mono.error(new ClientError(HttpStatus.BAD_REQUEST,
                                new ErrorRepresentation(new Error(ErrorCode.USER_NOT_FOUND, "Invalid patient")))))
                .map(Objects::nonNull);
    }

    private Mono<Boolean> validateHIPAndHIU(RequestedDetail requestedDetail) {
        Mono<Boolean> checkHIU = isValidHIU(requestedDetail).subscribeOn(Schedulers.elastic());
        Mono<Boolean> checkHIP = isValidHIP(requestedDetail).subscribeOn(Schedulers.elastic());
        return Mono.zip(checkHIU, checkHIP, (validHIU, validHIP) -> validHIU && validHIP);
    }

    private Mono<Void> saveRequest(RequestedDetail requestedDetail, String requestId) {
        return consentRequestRepository.insert(requestedDetail, requestId);
    }

    private Mono<Boolean> isValidHIU(RequestedDetail requestedDetail) {
        return centralRegistry.providerWith(getHIUId(requestedDetail))
                .flatMap(hiu -> Mono.just(hiu != null));
    }

    private Mono<Boolean> isValidHIP(RequestedDetail requestedDetail) {
        String hipId = getHIPId(requestedDetail);
        if (hipId != null) {
            return centralRegistry.providerWith(hipId)
                    .flatMap(hip -> Mono.just(hip != null));
        }
        return Mono.just(true);
    }

    private String getHIPId(RequestedDetail requestedDetail) {
        if (requestedDetail.getHip() != null) {
            return requestedDetail.getHip().getId();
        }
        return null;
    }

    private String getHIUId(RequestedDetail requestedDetail) {
        return requestedDetail.getHiu().getId();
    }

    public Mono<List<ConsentRequestDetail>> findRequestsForPatient(String patientId, int limit, int offset) {
        return consentRequestRepository.requestsForPatient(patientId, limit, offset);
    }

    private Mono<Void> validateLinkedHips(String username, List<GrantedConsent> grantedConsents) {
        return patientServiceClient.retrievePatientLinks(username)
                .flatMap(linkedCareContexts ->
                        Flux.fromIterable(grantedConsents)
                                .filter(grantedConsent ->
                                        linkedCareContexts.hasCCReferences(
                                                grantedConsent.getHip().getId(),
                                                grantedConsent.getCareContexts().stream()
                                                        .map(GrantedContext::getCareContextReference)
                                                        .collect(Collectors.toList())))
                                .collectList()
                                .map(filteredList -> filteredList.size() == grantedConsents.size()))
                .filter(result -> result)
                .switchIfEmpty(Mono.error(ClientError.invalidProviderOrCareContext()))
                .then();
    }

    public Mono<ConsentApprovalResponse> approveConsent(String patientId,
                                                        String requestId,
                                                        List<GrantedConsent> grantedConsents) {
        return validatePatient(patientId)
                .then(validateConsentRequest(requestId, patientId))
                .flatMap(consentRequest -> validateLinkedHips(patientId, grantedConsents)
                        .then(generateConsentArtefacts(requestId, grantedConsents, patientId, consentRequest)
                                .flatMap(consents ->
                                        broadcastConsentArtefacts(consents,
                                                consentRequest.getCallBackUrl(),
                                                requestId,
                                                ConsentStatus.GRANTED,
                                                consentRequest.getLastUpdated())
                                                .thenReturn(consentApprovalResponse(consents)))));
    }

    private Mono<Void> broadcastConsentArtefacts(List<HIPConsentArtefactRepresentation> consents,
                                                 String hiuCallBackUrl,
                                                 String requestId,
                                                 ConsentStatus status,
                                                 Date lastUpdated) {
        ConsentArtefactsMessage message = ConsentArtefactsMessage
                .builder()
                .status(status)
                .timestamp(lastUpdated)
                .consentRequestId(requestId)
                .consentArtefacts(consents)
                .hiuCallBackUrl(hiuCallBackUrl)
                .build();

        return consentNotificationPublisher.broadcastConsentArtefacts(message);
    }

    private ConsentApprovalResponse consentApprovalResponse(List<HIPConsentArtefactRepresentation> consentArtefacts) {
        List<ConsentReference> consents = consentArtefacts
                .stream()
                .map(this::from)
                .collect(Collectors.toList());

        return ConsentApprovalResponse.builder().consents(consents).build();
    }

    private ConsentReference from(HIPConsentArtefactRepresentation consent) {
        return ConsentReference
                .builder()
                .id(consent.getConsentDetail().getConsentId())
                .status(consent.getStatus())
                .build();
    }

    private Mono<List<HIPConsentArtefactRepresentation>> generateConsentArtefacts(String requestId,
                                                                                  List<GrantedConsent> grantedConsents,
                                                                                  String patientId,
                                                                                  ConsentRequestDetail consentRequest) {
        return Flux.fromIterable(grantedConsents)
                .flatMap(grantedConsent -> storeConsentArtefact(requestId, patientId, consentRequest, grantedConsent))
                .collectList();
    }

    private HIPConsentArtefactRepresentation from(ConsentArtefact consentArtefact, ConsentStatus status) {
        HIPConsentArtefact hipConsentArtefact = HIPConsentArtefact.builder()
                .consentId(consentArtefact.getConsentId())
                .createdAt(consentArtefact.getCreatedAt())
                .purpose(consentArtefact.getPurpose())
                .careContexts(consentArtefact.getCareContexts())
                .patient(consentArtefact.getPatient())
                .hip(consentArtefact.getHip())
                .hiTypes(consentArtefact.getHiTypes())
                .permission(consentArtefact.getPermission())
                .build();
        String signature = getConsentArtefactSignature(hipConsentArtefact);

        return HIPConsentArtefactRepresentation
                .builder()
                .consentDetail(hipConsentArtefact)
                .signature(signature)
                .status(status)
                .build();
    }

    private Mono<HIPConsentArtefactRepresentation> storeConsentArtefact(String requestId,
                                                                        String patientId,
                                                                        ConsentRequestDetail consentRequest,
                                                                        GrantedConsent grantedConsent) {
        var consentArtefact = from(consentRequest, grantedConsent);
        var hipConsentArtefact = from(consentArtefact, ConsentStatus.GRANTED);
        var consentArtefactSignature = getConsentArtefactSignature(consentArtefact);
        return consentArtefactRepository.addConsentArtefactAndUpdateStatus(consentArtefact,
                requestId,
                patientId,
                consentArtefactSignature,
                hipConsentArtefact)
                .thenReturn(hipConsentArtefact);
    }

    @SneakyThrows
    private String getConsentArtefactSignature(Serializable consentArtefact) {
        PrivateKey privateKey = keyPair.getPrivate();
        Signature signature = Signature.getInstance(SHA_1_WITH_RSA);
        SignedObject signedObject = new SignedObject(consentArtefact, privateKey, signature);
        return Base64.getEncoder().encodeToString(signedObject.getSignature());
    }

    private ConsentArtefact from(ConsentRequestDetail requestDetail, GrantedConsent granted) {
        PatientReference patientReference = PatientReference.builder().id(requestDetail.getPatient().getId()).build();
        String consentArtefactId = UUID.randomUUID().toString();
        //TODO: need to save also the CC
        return ConsentArtefact.builder()
                .consentId(consentArtefactId)
                .createdAt(new Date())
                .purpose(requestDetail.getPurpose())
                .careContexts(granted.getCareContexts())
                .patient(patientReference)
                .hiu(requestDetail.getHiu())
                .requester(requestDetail.getRequester())
                .hip(granted.getHip())
                .hiTypes(granted.getHiTypes())
                .permission(granted.getPermission())
                .build();
    }

    private Mono<ConsentRequestDetail> validateConsentRequest(String requestId, String patientId) {
        return consentRequestRepository.requestOf(requestId, ConsentStatus.REQUESTED.toString(), patientId)
                .switchIfEmpty(Mono.error(ClientError.consentRequestNotFound()));
    }

    public Mono<ConsentArtefactRepresentation> getConsent(String consentId, String requesterId) {
        return getConsentArtefact(consentId)
                .switchIfEmpty(Mono.error(ClientError.consentArtefactNotFound()))
                .flatMap(r -> {
                    if (isNotSameRequester(r.getConsentDetail(), requesterId)) {
                        return Mono.error(ClientError.consentArtefactForbidden());
                    }
                    return Mono.just(r);
                });
    }

    public Mono<ConsentArtefactLightRepresentation> getConsentArtefactLight(String consentId) {
        return getHipConsentArtefact(consentId)
                .flatMap(hipConsentArtefactRepresentation -> getConsentArtefact(consentId)
                        .flatMap(consentArtefactRepresentation ->
                                Mono.just(from(hipConsentArtefactRepresentation, consentArtefactRepresentation))));
    }

    private Mono<ConsentArtefactRepresentation> getConsentArtefact(String consentId) {
        return consentArtefactRepository.getConsentArtefact(consentId)
                .switchIfEmpty(Mono.error(ClientError.consentArtefactNotFound()));
    }

    private Mono<HIPConsentArtefactRepresentation> getHipConsentArtefact(String consentId) {
        return consentArtefactRepository.getHipConsentArtefact(consentId)
                .switchIfEmpty(Mono.error(ClientError.consentArtefactNotFound()));
    }

    private ConsentArtefactLightRepresentation from(HIPConsentArtefactRepresentation hipConsentArtefact,
                                                    ConsentArtefactRepresentation consentArtefact) {
        ConsentArtefactLight consentArtefactLight = ConsentArtefactLight.builder()
                .hiu(consentArtefact.getConsentDetail().getHiu())
                .permission(consentArtefact.getConsentDetail().getPermission())
                .build();
        return ConsentArtefactLightRepresentation.builder()
                .status(consentArtefact.getStatus())
                .consentDetail(consentArtefactLight)
                .signature(hipConsentArtefact.getSignature())
                .build();
    }

    public Flux<ConsentArtefactRepresentation> getConsents(String consentRequestId, String requesterId) {
        return consentArtefactRepository.getConsentArtefacts(consentRequestId)
                .flatMap(consentArtefactRepository::getConsentArtefact)
                .switchIfEmpty(Mono.error(ClientError.consentArtefactNotFound()))
                .filter(consentArtefact -> !isNotSameRequester(consentArtefact.getConsentDetail(), requesterId))
                .switchIfEmpty(Mono.error(ClientError.consentArtefactForbidden()));
    }

    public Mono<ConsentRepresentation> getConsentRepresentation(String consentId, String requesterId) {
        return getConsentWithRequest(consentId)
                .switchIfEmpty(Mono.error(ClientError.consentArtefactNotFound()))
                .flatMap(consentRepresentation -> {
                    if (isNotSameRequester(consentRepresentation.getConsentDetail(), requesterId)) {
                        return Mono.error(ClientError.consentArtefactForbidden());
                    }
                    return Mono.just(consentRepresentation);
                });
    }

    private Mono<ConsentRepresentation> getConsentWithRequest(String consentId) {
        return consentArtefactRepository.getConsentWithRequest(consentId)
                .switchIfEmpty(Mono.error(ClientError.consentArtefactNotFound()));
    }

    public Mono<List<HIPConsentArtefactRepresentation>> revokeConsent(RevokeRequest revokeRequest, String requesterId) {
        return Flux.fromIterable(revokeRequest.getConsents())
                .flatMap(consentId -> getConsentRepresentation(consentId, requesterId)
                        .flatMap(consentRepresentation -> consentRequestRepository.requestOf(
                                consentRepresentation.getConsentRequestId(),
                                consentRepresentation.getStatus().toString(),
                                consentRepresentation.getConsentDetail().getPatient().getId())
                                .flatMap(consentRequestDetail -> consentArtefactRepository.updateStatus(consentId,
                                        consentRepresentation.getConsentRequestId(),
                                        ConsentStatus.REVOKED)
                                        .thenReturn(from(consentRepresentation.getConsentDetail(), ConsentStatus.REVOKED))
                                )))
                .collectList();
    }

    public Mono<Void> revokeAndBroadCastConsent(RevokeRequest revokeRequest, String requesterId) {
        return Flux.fromIterable(revokeRequest.getConsents())
                .flatMap(consentId -> getConsentRepresentation(consentId, requesterId)
                        .flatMap(consentRepresentation ->
                                consentRequestRepository.requestOf(
                                        consentRepresentation.getConsentRequestId(),
                                        ConsentStatus.GRANTED.toString(),
                                        consentRepresentation.getConsentDetail().getPatient().getId())
                                .flatMap(consentRequestDetail -> revokeConsent(revokeRequest, requesterId)
                                        .flatMap(hipConsentArtefactRepresentations ->
                                                broadcastConsentArtefacts(
                                                        hipConsentArtefactRepresentations,
                                                        consentRequestDetail.getCallBackUrl(),
                                                        "",
                                                        ConsentStatus.REVOKED,
                                                        consentRepresentation.getDateModified()))))).then();
    }
}