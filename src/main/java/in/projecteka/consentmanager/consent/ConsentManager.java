package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.ConsentManagerClient;
import in.projecteka.consentmanager.clients.PatientServiceClient;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.clients.model.Error;
import in.projecteka.consentmanager.clients.model.ErrorRepresentation;
import in.projecteka.consentmanager.common.CentralRegistry;
import in.projecteka.consentmanager.consent.model.CMReference;
import in.projecteka.consentmanager.consent.model.HIUReference;
import in.projecteka.consentmanager.consent.model.Consent;
import in.projecteka.consentmanager.consent.model.ConsentArtefact;
import in.projecteka.consentmanager.consent.model.ConsentArtefactResult;
import in.projecteka.consentmanager.consent.model.ConsentArtefactsMessage;
import in.projecteka.consentmanager.consent.model.ConsentDetail;
import in.projecteka.consentmanager.consent.model.ConsentPurpose;
import in.projecteka.consentmanager.consent.model.ConsentRepresentation;
import in.projecteka.consentmanager.consent.model.ConsentRequest;
import in.projecteka.consentmanager.consent.model.ConsentRequestDetail;
import in.projecteka.consentmanager.consent.model.ConsentStatus;
import in.projecteka.consentmanager.consent.model.GrantedContext;
import in.projecteka.consentmanager.consent.model.HIPConsentArtefact;
import in.projecteka.consentmanager.consent.model.HIPConsentArtefactRepresentation;
import in.projecteka.consentmanager.consent.model.HIType;
import in.projecteka.consentmanager.consent.model.ListResult;
import in.projecteka.consentmanager.consent.model.PatientReference;
import in.projecteka.consentmanager.consent.model.QueryRepresentation;
import in.projecteka.consentmanager.consent.model.RevokeRequest;
import in.projecteka.consentmanager.consent.model.request.GrantedConsent;
import in.projecteka.consentmanager.consent.model.request.RequestedDetail;
import in.projecteka.consentmanager.consent.model.response.ConsentApprovalResponse;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactLight;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactLightRepresentation;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactRepresentation;
import in.projecteka.consentmanager.consent.model.response.ConsentReference;
import in.projecteka.consentmanager.consent.model.response.ConsentRequestId;
import in.projecteka.consentmanager.consent.model.response.ConsentRequestResult;
import in.projecteka.consentmanager.link.discovery.model.patient.response.GatewayResponse;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static in.projecteka.consentmanager.clients.model.ErrorCode.CONSENT_REQUEST_NOT_FOUND;
import static in.projecteka.consentmanager.clients.model.ErrorCode.INVALID_HITYPE;
import static in.projecteka.consentmanager.clients.model.ErrorCode.INVALID_PURPOSE;
import static in.projecteka.consentmanager.clients.model.ErrorCode.INVALID_STATE;
import static in.projecteka.consentmanager.clients.model.ErrorCode.USER_NOT_FOUND;
import static in.projecteka.consentmanager.consent.model.ConsentStatus.DENIED;
import static in.projecteka.consentmanager.consent.model.ConsentStatus.GRANTED;
import static in.projecteka.consentmanager.consent.model.ConsentStatus.REQUESTED;
import static in.projecteka.consentmanager.consent.model.ConsentStatus.REVOKED;
import static java.lang.String.format;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@AllArgsConstructor
public class ConsentManager {

    public static final String SHA_1_WITH_RSA = "SHA1withRSA";
    public static final String ALL_CONSENT_ARTEFACTS = "ALL";
    private final UserServiceClient userServiceClient;
    private final ConsentRequestRepository consentRequestRepository;
    private final ConsentArtefactRepository consentArtefactRepository;
    private final KeyPair keyPair;
    private final ConsentNotificationPublisher consentNotificationPublisher;
    private final CentralRegistry centralRegistry;
    private final PostConsentRequest postConsentRequest;
    private final PatientServiceClient patientServiceClient;
    private final CMProperties cmProperties;
    private final ConceptValidator conceptValidator;
    private final ConsentArtefactQueryGenerator consentArtefactQueryGenerator;
    private final ConsentManagerClient consentManagerClient;

    private static boolean isSameRequester(ConsentArtefact consentDetail, String requesterId) {
        return consentDetail.getHiu().getId().equals(requesterId) ||
                consentDetail.getPatient().getId().equals(requesterId);
    }

    public Mono<String> askForConsent(RequestedDetail requestedDetail, UUID requestId) {
        return Mono.just(requestId)
                .filterWhen(this::validateRequest)
                .switchIfEmpty(Mono.error(ClientError.requestAlreadyExists()))
                .flatMap(val -> validatePatient(requestedDetail.getPatient().getId())
                        .then(validatePurpose(requestedDetail.getPurpose()))
                        .then(validateHiTypes(requestedDetail.getHiTypes()))
                        .then(validateHIPAndHIU(requestedDetail))
                        .then(saveRequest(requestedDetail, requestId))
                        .then(postConsentRequest.broadcastConsentRequestNotification(ConsentRequest.builder()
                                .detail(requestedDetail)
                                .id(requestId)
                                .build()))
                        .thenReturn(requestId.toString()));
    }

    public Mono<Void> requestConsent(RequestedDetail requestedDetail, UUID requestId) {
        return Mono.just(requestId)
                .filterWhen(this::validateRequest)
                .switchIfEmpty(Mono.error(ClientError.requestAlreadyExists()))
                .flatMap(val -> validatePatient(requestedDetail.getPatient().getId())
                        .then(validatePurpose(requestedDetail.getPurpose()))
                        .then(validateHiTypes(requestedDetail.getHiTypes()))
                        .then(validateHIPAndHIU(requestedDetail)))
                .flatMap(r -> Mono.defer(() -> saveConsentRequest(requestedDetail, requestId)));
    }


    private Mono<Void> saveConsentRequest(RequestedDetail requestedDetail, UUID requestId) {
        ConsentRequestId request = ConsentRequestId.builder()
                .id(requestId)
                .build();
        ConsentRequestResult consentRequestResult = ConsentRequestResult.builder()
                .requestId(requestId)
                .timestamp(Instant.now().toString())
                .consentRequest(request)
                .resp(GatewayResponse.builder().requestId(UUID.randomUUID().toString()).build())
                .build();

        return consentRequestRepository.insert(requestedDetail, requestId)
                .doOnSuccess(s -> Mono.defer(() -> consentManagerClient
                        .sendInitResponseToGateway(consentRequestResult, requestedDetail.getHiu().getId())).subscribe());
    }

    private Mono<Boolean> validateRequest(UUID requestId) {
        return consentRequestRepository.requestOf(requestId.toString())
                .map(Objects::isNull)
                .switchIfEmpty(Mono.just(true));
    }

    private Mono<Void> validatePurpose(ConsentPurpose purpose) {
        return conceptValidator.validatePurpose(purpose.getCode())
                .filter(Predicate.isEqual(false))
                .flatMap(invalidPurposeExists -> Mono.error(new ClientError(BAD_REQUEST,
                        new ErrorRepresentation(new Error(INVALID_PURPOSE, "Invalid Purpose")))));
    }

    private Mono<Void> validateHiTypes(HIType[] hiTypes) {
        return conceptValidator.validateHITypes(Arrays.stream(hiTypes)
                .map(HIType::getValue)
                .collect(Collectors.toList()))
                .filter(Predicate.isEqual(false))
                .flatMap(invalidHiTypesExists ->
                        Mono.error(new ClientError(BAD_REQUEST,
                                new ErrorRepresentation(new Error(INVALID_HITYPE, "Invalid HI Type")))));
    }

    private Mono<Boolean> validatePatient(String patientId) {
        return userServiceClient.userOf(patientId)
                .onErrorResume(ClientError.class,
                        clientError -> Mono.error(new ClientError(BAD_REQUEST,
                                new ErrorRepresentation(new Error(USER_NOT_FOUND, "Invalid patient")))))
                .map(Objects::nonNull);
    }

    private Mono<Boolean> validateHIPAndHIU(RequestedDetail requestedDetail) {
        Mono<Boolean> checkHIU = isValidProvider(requestedDetail.getHIUId()).subscribeOn(Schedulers.elastic());
        Mono<Boolean> checkHIP = isValidHIP(requestedDetail).subscribeOn(Schedulers.elastic());
        return Mono.zip(checkHIU, checkHIP, (validHIU, validHIP) -> validHIU && validHIP);
    }

    private Mono<Void> saveRequest(RequestedDetail requestedDetail, UUID requestId) {
        return consentRequestRepository.insert(requestedDetail, requestId);
    }

    private Mono<Boolean> isValidHIP(RequestedDetail requestedDetail) {
        return requestedDetail.getHIPId()
                .map(this::isValidProvider)
                .orElse(Mono.just(true));
    }

    private Mono<Boolean> isValidProvider(String providerId) {
        return centralRegistry.providerWith(providerId).map(Objects::nonNull);
    }

    public Mono<ListResult<List<ConsentRequestDetail>>> findRequestsForPatient(String patientId,
                                                                               int limit,
                                                                               int offset,
                                                                               String status) {
        return ALL_CONSENT_ARTEFACTS.equals(status)
                ? consentRequestRepository.requestsForPatient(patientId, limit, offset, null)
                : consentRequestRepository.requestsForPatient(patientId, limit, offset, status);
    }

    private Mono<Void> validateLinkedHips(String username, List<GrantedConsent> grantedConsents) {
        return patientServiceClient.retrievePatientLinks(username)
                .flatMap(linkedCareContexts ->
                        Flux.fromIterable(grantedConsents)
                                .filter(grantedConsent ->
                                        linkedCareContexts.hasCCReferences(
                                                grantedConsent.getHip().getId(),
                                                careContextsIn(grantedConsent)))
                                .collectList()
                                .filter(filteredList -> filteredList.size() == grantedConsents.size()))
                .switchIfEmpty(Mono.error(ClientError.invalidProviderOrCareContext()))
                .then();
    }

    private List<String> careContextsIn(GrantedConsent grantedConsent) {
        return grantedConsent.getCareContexts().stream()
                .map(GrantedContext::getCareContextReference)
                .collect(Collectors.toList());
    }

    public Mono<ConsentApprovalResponse> approveConsent(String patientId,
                                                        String requestId,
                                                        List<GrantedConsent> grantedConsents) {
        return validatePatient(patientId)
                .then(validateHiTypes(in(grantedConsents)))
                .then(validateConsentRequest(requestId, patientId))
                .flatMap(consentRequest -> validateLinkedHips(patientId, grantedConsents)
                        .then(generateConsentArtefacts(requestId, grantedConsents, patientId, consentRequest)
                                .flatMap(consents ->
                                        broadcastConsentArtefacts(consents,
                                                consentRequest.getConsentNotificationUrl(),
                                                requestId,
                                                GRANTED,
                                                consentRequest.getLastUpdated(),
                                                consentRequest.getHiu())
                                                .thenReturn(consentApprovalResponse(consents)))));
    }

    private Mono<ConsentArtefactRepresentation> updateHipName(
            ConsentArtefactRepresentation consentArtefactRepresentation) {
        return centralRegistry.providerWith(consentArtefactRepresentation.getConsentDetail().getHip().getId())
                .map(provider -> {
                    consentArtefactRepresentation.getConsentDetail().getHip().setName(provider.getName());
                    return consentArtefactRepresentation;
                });
    }

    private HIType[] in(List<GrantedConsent> grantedConsents) {
        return grantedConsents.stream()
                .parallel()
                .flatMap(grantedConsent -> Arrays.stream(grantedConsent.getHiTypes()))
                .toArray(HIType[]::new);
    }

    private Mono<Void> broadcastConsentArtefacts(List<HIPConsentArtefactRepresentation> consents,
                                                 String hiuConsentNotificationUrl,
                                                 String requestId,
                                                 ConsentStatus status,
                                                 LocalDateTime lastUpdated,
                                                 HIUReference consentRequest) {
        ConsentArtefactsMessage message = ConsentArtefactsMessage
                .builder()
                .status(status)
                .timestamp(lastUpdated)
                .consentRequestId(requestId)
                .consentArtefacts(consents)
                .hiuConsentNotificationUrl(hiuConsentNotificationUrl)
                .hiuId(consentRequest.getId())
                .build();
        return consentNotificationPublisher.publish(message);
    }

    private ConsentApprovalResponse consentApprovalResponse(
            List<HIPConsentArtefactRepresentation> consentArtefacts) {
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
        return getAllQueries(requestId, grantedConsents, patientId, consentRequest)
                .map(caQueries -> caQueries.stream().reduce(QueryRepresentation::add).get())
                .flatMap(queryRepresentation -> consentArtefactRepository.process(queryRepresentation.getQueries())
                        .thenReturn(queryRepresentation.getHipConsentArtefactRepresentations()));
    }

    private Mono<List<QueryRepresentation>> getAllQueries(String requestId,
                                                          List<GrantedConsent> grantedConsents,
                                                          String patientId,
                                                          ConsentRequestDetail consentRequest) {
        return Flux.fromIterable(grantedConsents)
                .flatMap(grantedConsent -> toConsentArtefact(consentRequest, grantedConsent)
                        .flatMap(consentArtefact -> consentArtefactQueryGenerator.toQueries(requestId,
                                patientId,
                                consentArtefact,
                                from(consentArtefact, GRANTED),
                                signConsentArtefact(consentArtefact))))
                .collectList();
    }

    private Mono<ConsentArtefact> toConsentArtefact(ConsentRequestDetail requestDetail, GrantedConsent grantedConsent) {
        return Mono.just(from(requestDetail, grantedConsent));
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
                .consentManager(getConsentManagerRef())
                .build();
        String signature = signConsentArtefact(hipConsentArtefact);

        return HIPConsentArtefactRepresentation
                .builder()
                .consentId(consentArtefact.getConsentId())
                .consentDetail(hipConsentArtefact)
                .signature(signature)
                .status(status)
                .build();
    }

    @SneakyThrows
    private String signConsentArtefact(Serializable consentArtefact) {
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
                .createdAt(LocalDateTime.now())
                .purpose(requestDetail.getPurpose())
                .careContexts(granted.getCareContexts())
                .patient(patientReference)
                .hiu(requestDetail.getHiu())
                .requester(requestDetail.getRequester())
                .hip(granted.getHip())
                .hiTypes(granted.getHiTypes())
                .permission(granted.getPermission())
                .consentManager(getConsentManagerRef())
                .build();
    }

    private CMReference getConsentManagerRef() {
        return CMReference.builder().id(cmProperties.getId()).build();
    }

    private Mono<ConsentRequestDetail> validateConsentRequest(String requestId, String patientId) {
        return consentRequestRepository.requestOf(requestId, REQUESTED.toString(), patientId)
                .switchIfEmpty(Mono.error(ClientError.consentRequestNotFound()));
    }

    public Mono<ConsentArtefactRepresentation> getConsent(String consentId, String requesterId) {
        return getConsentArtefact(consentId)
                .switchIfEmpty(Mono.error(ClientError.consentArtefactNotFound()))
                .flatMap(this::updateHipName)
                .filter(artefact -> isSameRequester(artefact.getConsentDetail(), requesterId))
                .switchIfEmpty(Mono.error(ClientError.consentArtefactForbidden()));
    }

    public Mono<Void> getConsent(String consentId, UUID requestId, String hiuId) {
        return getConsentArtefact(consentId)
                .filter(artefact -> isSameRequester(artefact.getConsentDetail(), hiuId))
                .switchIfEmpty(Mono.error(ClientError.consentArtefactForbidden()))
                .flatMap(this::updateHipName)
                .map(artefact -> {
                    ConsentArtefact consentArtefact = artefact.getConsentDetail();
                    ConsentDetail consentDetail = ConsentDetail.builder()
                            .consentId(consentArtefact.getConsentId())
                            .createdAt(consentArtefact.getCreatedAt())
                            .patient(consentArtefact.getPatient())
                            .careContexts(consentArtefact.getCareContexts())
                            .purpose(consentArtefact.getPurpose())
                            .hip(consentArtefact.getHip())
                            .hiu(consentArtefact.getHiu())
                            .consentManager(consentArtefact.getConsentManager())
                            .requester(consentArtefact.getRequester())
                            .hiTypes(consentArtefact.getHiTypes())
                            .permission(consentArtefact.getPermission())
                            .build();
                    Consent consent = Consent.builder()
                            .status(artefact.getStatus())
                            .consentDetail(consentDetail)
                            .signature(artefact.getSignature())
                            .build();
                    return ConsentArtefactResult.builder()
                            .requestId(UUID.randomUUID())
                            .timestamp(Instant.now().toString())
                            .consent(consent)
                            .resp(GatewayResponse.builder().requestId(requestId.toString()).build())
                            .build();
                })
                .onErrorResume(ClientError.class, exception -> {
                    var consentArtefactResult = ConsentArtefactResult.builder()
                            .requestId(UUID.randomUUID())
                            .timestamp(Instant.now().toString())
                            .error(ClientError.from(exception))
                            .resp(GatewayResponse.builder().requestId(requestId.toString()).build())
                            .build();
                    return Mono.just(consentArtefactResult);
                })
                .flatMap(consentArtefact -> consentArtefactResponse(consentArtefact, hiuId));
    }

    private Mono<Void> consentArtefactResponse(ConsentArtefactResult consentArtefactResult, String hiuId) {
        return consentManagerClient.sendConsentArtefactResponseToGateway(consentArtefactResult, hiuId);
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
                .filter(consentArtefact -> isSameRequester(consentArtefact.getConsentDetail(), requesterId))
                .switchIfEmpty(Mono.error(ClientError.consentArtefactForbidden()));
    }

    public Mono<ConsentRepresentation> getGrantedConsentRepresentation(String consentId, String requesterId) {
        return getConsentRepresentation(consentId, requesterId)
                .filter(this::isGrantedConsent)
                .switchIfEmpty(Mono.error(ClientError.consentNotGranted()));
    }

    private Mono<ConsentRepresentation> getConsentRepresentation(String consentId, String requesterId) {
        return getConsentWithRequest(consentId)
                .filter(consent -> isSameRequester(consent.getConsentDetail(), requesterId))
                .switchIfEmpty(Mono.error(ClientError.consentArtefactForbidden()));
    }

    private boolean isGrantedConsent(ConsentRepresentation consentRepresentation) {
        return consentRepresentation.getStatus().equals(GRANTED);
    }

    private Mono<ConsentRepresentation> getConsentWithRequest(String consentId) {
        return consentArtefactRepository.getConsentWithRequest(consentId)
                .switchIfEmpty(Mono.error(ClientError.consentArtefactNotFound()));
    }

    public Mono<List<HIPConsentArtefactRepresentation>> getHIPConsentArtefacts(RevokeRequest revokeRequest,
                                                                               String requesterId) {
        return Flux.fromIterable(revokeRequest.getConsents())
                .flatMap(consentId -> getConsentRepresentation(consentId, requesterId)
                        .map(consentRepresentation ->
                                from(consentRepresentation.getConsentDetail(), REVOKED)))
                .collectList();
    }

    public Mono<Void> revoke(RevokeRequest revokeRequest, String requesterId) {
        return Flux.fromIterable(revokeRequest.getConsents())
                .flatMap(consentId -> getGrantedConsentRepresentation(consentId, requesterId)
                        .flatMap(consentRepresentation -> consentRequestRepository.requestOf(
                                consentRepresentation.getConsentRequestId(),
                                GRANTED.toString(),
                                consentRepresentation.getConsentDetail().getPatient().getId())
                                .flatMap(consentRequestDetail -> updateStatusAndBroadcast(revokeRequest, requesterId,
                                        consentId, consentRepresentation, consentRequestDetail)))).then();
    }

    private Mono<Void> updateStatusAndBroadcast(RevokeRequest revokeRequest,
                                                String requesterId,
                                                String consentId,
                                                ConsentRepresentation consentRepresentation,
                                                ConsentRequestDetail consentRequestDetail) {
        return consentArtefactRepository.updateStatus(
                consentId,
                consentRepresentation.getConsentRequestId(),
                REVOKED)
                .then(getHIPConsentArtefacts(revokeRequest, requesterId))
                .flatMap(hipConsentArtefactRepresentations -> broadcastConsentArtefacts(
                        hipConsentArtefactRepresentations,
                        consentRequestDetail.getConsentNotificationUrl(),
                        "",
                        REVOKED,
                        consentRepresentation.getDateModified(),
                        consentRequestDetail.getHiu()));
    }

    public Mono<Void> deny(String id, String patientId) {
        return consentRequestRepository.requestOf(id)
                .switchIfEmpty(Mono.error(new ClientError(NOT_FOUND,
                        new ErrorRepresentation(new Error(CONSENT_REQUEST_NOT_FOUND,
                                "Consent request not existing")))))
                .filter(consentRequest -> consentRequest.getPatient().getId().equals(patientId))
                .switchIfEmpty(Mono.error(new ClientError(FORBIDDEN,
                        new ErrorRepresentation(new Error(CONSENT_REQUEST_NOT_FOUND,
                                format("Consent request not existing for patient: %s", patientId))))))
                .filter(consentRequest -> consentRequest.getStatus().equals(REQUESTED))
                .switchIfEmpty(Mono.error(new ClientError(CONFLICT,
                        new ErrorRepresentation(new Error(INVALID_STATE,
                                format("Consent request is not in %s state", REQUESTED.toString()))))))
                .flatMap(consentRequest -> consentRequestRepository.updateStatus(id, DENIED)
                        .then(consentRequestRepository.requestOf(id)))
                .flatMap(consentRequest -> broadcastConsentArtefacts(List.of(),
                        consentRequest.getConsentNotificationUrl(),
                        consentRequest.getRequestId(),
                        consentRequest.getStatus(),
                        consentRequest.getLastUpdated(),
                        consentRequest.getHiu()));
    }

    public Mono<ListResult<List<ConsentArtefactRepresentation>>> getAllConsentArtefacts(String username,
                                                                                        int limit,
                                                                                        int offset,
                                                                                        String status) {
        return status.equals(ALL_CONSENT_ARTEFACTS)
                ? consentArtefactRepository.getAllConsentArtefacts(username, limit, offset, null)
                : consentArtefactRepository.getAllConsentArtefacts(username, limit, offset, status);
    }
}