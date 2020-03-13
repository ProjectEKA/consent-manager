package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.PatientServiceClient;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.common.CentralRegistry;
import in.projecteka.consentmanager.consent.model.ConsentArtefact;
import in.projecteka.consentmanager.consent.model.ConsentArtefactsMessage;
import in.projecteka.consentmanager.consent.model.ConsentRequest;
import in.projecteka.consentmanager.consent.model.ConsentRequestDetail;
import in.projecteka.consentmanager.consent.model.ConsentStatus;
import in.projecteka.consentmanager.consent.model.HIPConsentArtefact;
import in.projecteka.consentmanager.consent.model.HIPConsentArtefactRepresentation;
import in.projecteka.consentmanager.consent.model.PatientReference;
import in.projecteka.consentmanager.consent.model.request.GrantedConsent;
import in.projecteka.consentmanager.consent.model.request.RequestedDetail;
import in.projecteka.consentmanager.consent.model.response.ConsentApprovalResponse;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactLight;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactLightRepresentation;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactReference;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactRepresentation;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
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
    private final PostConsentApproval postConsentApproval;
    private final CentralRegistry centralRegistry;
    private final PostConsentRequest postConsentRequest;
    private final PatientServiceClient patientServiceClient;

    private static boolean isNotSameRequester(ConsentArtefact consentDetail, String requesterId) {
        return !consentDetail.getHiu().getId().equals(requesterId) &&
                !consentDetail.getPatient().getId().equals(requesterId);
    }

    public Mono<String> askForConsent(RequestedDetail requestedDetail) {
        final String requestId = UUID.randomUUID().toString();
        return Mono.subscriberContext()
                .flatMap(context -> validatePatient(requestedDetail.getPatient().getId(), context.get("Authorization")))
                .then(validateHIPAndHIU(requestedDetail))
                .then(saveRequest(requestedDetail, requestId))
                .then(postConsentRequest.broadcastConsentRequestNotification(ConsentRequest.builder()
                        .detail(requestedDetail)
                        .id(requestId)
                        .build()))
                .thenReturn(requestId);
    }

    private Mono<Boolean> validatePatient(String patientId, String token) {
        return userServiceClient.userOf(patientId)
                .map(Objects::nonNull)
                .subscriberContext(context -> context.put("Authorization", token));
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

    private Mono<Void> validateLinkedHips(String authorizationToken, List<GrantedConsent> grantedConsents) {
        return patientServiceClient.retrievePatientLinks(authorizationToken)
                .flatMap(linkedCareContexts ->
                        Flux.fromIterable(grantedConsents)
                                .filter(grantedConsent ->
                                        linkedCareContexts.hasCCReferences(
                                                grantedConsent.getHip().getId(),
                                                grantedConsent.getCareContexts().stream()
                                                        .map(c -> c.getCareContextReference()).collect(Collectors.toList())))
                                .collectList()
                                .map(filteredList -> filteredList.size() == grantedConsents.size())
                ).filter(result -> result)
                .switchIfEmpty(Mono.error(ClientError.invalidProviderOrCareContext()))
                .then();
    }

    public Mono<ConsentApprovalResponse> approveConsent(String patientId,
                                                        String requestId,
                                                        List<GrantedConsent> grantedConsents) {
        return Mono.subscriberContext()
                .flatMap(context -> validatePatient(patientId, context.get("Authorization")))
                .then(Mono.subscriberContext().map(context -> validateLinkedHips(context.get("Authorization"), grantedConsents)))
                .then(validateConsentRequest(requestId))
                .flatMap(consentRequest ->
                        generateConsentArtefacts(requestId, grantedConsents, patientId, consentRequest)
                                .flatMap(consents ->
                                        broadcastConsentArtefacts(consents, consentRequest.getCallBackUrl(), requestId)
                                                .thenReturn(consentApprovalResponse(consents))));
    }

    private Mono<Void> broadcastConsentArtefacts(List<HIPConsentArtefactRepresentation> consents,
                                                 String hiuCallBackUrl,
                                                 String requestId) {
        ConsentArtefactsMessage message = ConsentArtefactsMessage
                .builder()
                .consentArtefacts(consents)
                .hiuCallBackUrl(hiuCallBackUrl)
                .requestId(requestId)
                .build();

        return postConsentApproval.broadcastConsentArtefacts(message);
    }

    private ConsentApprovalResponse consentApprovalResponse(List<HIPConsentArtefactRepresentation> consentArtefacts) {
        List<ConsentArtefactReference> consents = consentArtefacts
                .stream()
                .map(this::from)
                .collect(Collectors.toList());

        return ConsentApprovalResponse.builder().consents(consents).build();
    }

    private ConsentArtefactReference from(HIPConsentArtefactRepresentation consent) {
        return ConsentArtefactReference
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

    private HIPConsentArtefactRepresentation from(ConsentArtefact consentArtefact) {
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
                .status(ConsentStatus.GRANTED)
                .build();
    }

    private Mono<HIPConsentArtefactRepresentation> storeConsentArtefact(String requestId,
                                                                        String patientId,
                                                                        ConsentRequestDetail consentRequest,
                                                                        GrantedConsent grantedConsent) {
        var consentArtefact = from(consentRequest, grantedConsent);
        var hipConsentArtefact = from(consentArtefact);
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

    private Mono<ConsentRequestDetail> validateConsentRequest(String requestId) {
        return consentRequestRepository.requestOf(requestId, ConsentStatus.REQUESTED.toString())
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
}