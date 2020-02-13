package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.ClientRegistryClient;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.consent.model.ConsentArtefact;
import in.projecteka.consentmanager.consent.model.ConsentArtefactsNotificationMessage;
import in.projecteka.consentmanager.consent.model.ConsentRequestDetail;
import in.projecteka.consentmanager.consent.model.ConsentStatus;
import in.projecteka.consentmanager.consent.model.HIPConsentArtefact;
import in.projecteka.consentmanager.consent.model.HIPConsentArtefactRepresentation;
import in.projecteka.consentmanager.consent.model.PatientReference;
import in.projecteka.consentmanager.consent.model.request.GrantedConsent;
import in.projecteka.consentmanager.consent.model.request.RequestedDetail;
import in.projecteka.consentmanager.consent.model.response.ConsentApprovalResponse;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactReference;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactRepresentation;
import in.projecteka.consentmanager.consent.repository.ConsentArtefactRepository;
import in.projecteka.consentmanager.consent.repository.ConsentRequestRepository;
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
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.Date;
import java.util.stream.Collectors;

@AllArgsConstructor
public class ConsentManager {

    public static final String SHA_1_WITH_RSA = "SHA1withRSA";
    private final ClientRegistryClient providerClient;
    private UserServiceClient userServiceClient;
    private final ConsentRequestRepository consentRequestRepository;
    private final ConsentArtefactRepository consentArtefactRepository;
    private KeyPair keyPair;
    private PostConsentApproval postConsentApproval;

    public Mono<String> askForConsent(String requestingHIUId, RequestedDetail requestedDetail) {
        final String requestId = UUID.randomUUID().toString();
        return validatePatient(requestedDetail.getPatient().getId())
                .then(validateHIPAndHIU(requestedDetail))
                .then(saveRequest(requestedDetail, requestId))
                .thenReturn(requestId);
    }

    private Mono<Boolean> validatePatient(String patientId) {
        return userServiceClient.userOf(patientId).map(Objects::nonNull);
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
        return providerClient.providerWith(getHIUId(requestedDetail)).flatMap(hiu -> Mono.just(hiu != null));
    }

    private Mono<Boolean> isValidHIP(RequestedDetail requestedDetail) {
        String hipId = getHIPId(requestedDetail);
        if (hipId != null) {
            return providerClient.providerWith(hipId).flatMap(hip -> Mono.just(hip != null));
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

    public Mono<ConsentApprovalResponse> approveConsent(String patientId,
                                                        String requestId,
                                                        List<GrantedConsent> grantedConsents) {
        return validatePatient(patientId)
                .then(validateConsentRequest(requestId))
                .flatMap(consentRequest ->
                        generateConsentArtefacts(requestId, grantedConsents, patientId, consentRequest)
                                .flatMap(consents ->
                                        broadcastConsentArtefact(consents, consentRequest.getCallBackUrl(), requestId)
                                                .thenReturn(consentApprovalResponse(consents))));
    }

    private Mono<Void> broadcastConsentArtefact(List<HIPConsentArtefactRepresentation> consents,
                                                String hiuCallBackUrl,
                                                String requestId) {
        ConsentArtefactsNotificationMessage message = ConsentArtefactsNotificationMessage
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
        String consentArtefactSignature = getConsentArtefactSignature(consentArtefact);

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
        //TODO: need to store also the CC
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
        return consentArtefactRepository.getConsentArtefact(consentId)
                .switchIfEmpty(Mono.error(ClientError.consentArtefactNotFound()))
                .flatMap(r -> {
                    if (!isValidRequester(r.getConsentDetail(), requesterId)) {
                        return Mono.error(ClientError.consentArtefactForbidden());
                    }
                    return Mono.just(r);
                });
    }

    private static boolean isValidRequester(ConsentArtefact consentDetail, String requesterId) {
        return consentDetail.getHiu().getId().equals(requesterId) || consentDetail.getPatient().getId().equals(requesterId);
    }

    public Mono<ConsentArtefactRepresentation> getConsentArtefact(String consentId) {
        return consentArtefactRepository.getConsentArtefact(consentId)
                .switchIfEmpty(Mono.error(ClientError.consentArtefactNotFound()));
    }
}