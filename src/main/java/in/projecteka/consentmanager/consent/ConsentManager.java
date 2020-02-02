package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.ClientRegistryClient;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.common.TokenUtils;
import in.projecteka.consentmanager.consent.model.ConsentArtefact;
import in.projecteka.consentmanager.consent.model.ConsentRequestDetail;
import in.projecteka.consentmanager.consent.model.ConsentStatus;
import in.projecteka.consentmanager.consent.model.LinkedContext;
import in.projecteka.consentmanager.consent.model.PatientLinkedContext;
import in.projecteka.consentmanager.consent.model.request.GrantedConsent;
import in.projecteka.consentmanager.consent.model.request.RequestedDetail;
import in.projecteka.consentmanager.consent.model.response.ConsentApprovalResponse;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactReference;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactRepresentation;
import in.projecteka.consentmanager.consent.repository.ConsentArtefactRepository;
import in.projecteka.consentmanager.consent.repository.ConsentRequestRepository;
import lombok.SneakyThrows;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignedObject;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ConsentManager {

    public static final String SHA_1_WITH_RSA = "SHA1withRSA";
    private final ClientRegistryClient providerClient;
    private UserServiceClient userServiceClient;
    private final ConsentRequestRepository consentRequestRepository;
    private final ConsentArtefactRepository consentArtefactRepository;
    private KeyPair keyPair;

    public ConsentManager(ConsentRequestRepository consentRequestRepository,
                          ClientRegistryClient providerClient,
                          UserServiceClient userServiceClient,
                          ConsentArtefactRepository consentArtefactRepository,
                          KeyPair keyPair) {
        this.consentRequestRepository = consentRequestRepository;
        this.providerClient = providerClient;
        this.userServiceClient = userServiceClient;
        this.consentArtefactRepository = consentArtefactRepository;
        this.keyPair = keyPair;
    }

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

    public Mono<ConsentApprovalResponse> approveConsent(String authorization,
                                                        String requestId,
                                                        List<GrantedConsent> grantedConsents) {
        String patientId = TokenUtils.readUserId(authorization);
        //TODO validate Carecontexts for the patient
        //also need to fetch HIP patient reference
        return validatePatient(patientId)
                .then(validateConsentRequest(requestId))
                .flatMap(consentRequest ->
                        Flux.fromIterable(grantedConsents)
                                .flatMap(grantedConsent -> {
                                    var consentArtefact = mapToConsentArtefact(consentRequest, grantedConsent);
                                    String consentArtefactSignature = getConsentArtefactSignature(consentArtefact);
                                    return consentArtefactRepository.addConsentArtefactAndUpdateStatus(consentArtefact, requestId, patientId, consentArtefactSignature)
                                            .thenReturn(ConsentArtefactReference.builder().id(consentArtefact.getConsentId()).build());
                                }).collectList())
                .map(consents -> ConsentApprovalResponse.builder().consents(consents).build());
    }

    @SneakyThrows
    private String getConsentArtefactSignature(ConsentArtefact consentArtefact) {
        PrivateKey privateKey = keyPair.getPrivate();
        Signature signature = Signature.getInstance(SHA_1_WITH_RSA);
        SignedObject signedObject = new SignedObject(consentArtefact, privateKey, signature);
        return Base64.getEncoder().encodeToString(signedObject.getSignature());
    }

    private ConsentArtefact mapToConsentArtefact(ConsentRequestDetail requestDetail, GrantedConsent granted) {
        PatientLinkedContext linkedPatientInfo = PatientLinkedContext.builder()
                .id(requestDetail.getPatient().getId())
                .careContexts(getLinkedCareContext(granted, requestDetail.getPatient().getId()))
                .build();
        String consentArtefactId = UUID.randomUUID().toString();
        //TODO: need to store also the CC
        return ConsentArtefact.builder()
                .consentId(consentArtefactId)
                .createdAt(new Date())
                .purpose(requestDetail.getPurpose())
                .patient(linkedPatientInfo)
                .hiu(requestDetail.getHiu())
                .requester(requestDetail.getRequester())
                .hip(granted.getHip())
                .hiTypes(granted.getHiTypes())
                .permission(granted.getPermission())
                .build();
    }

    private LinkedContext[] getLinkedCareContext(GrantedConsent granted, String id) {
        //TODO - should include HIP patient reference
        LinkedContext[] linkedContexts = granted.getCareContexts().stream().map(cc -> {
            return LinkedContext.builder().contextReference(cc.getContextReference()).build();
        }).toArray(LinkedContext[]::new);
        return linkedContexts;
    }

    private Mono<ConsentRequestDetail> validateConsentRequest(String requestId) {
        return consentRequestRepository.requestOf(requestId, ConsentStatus.REQUESTED.toString())
                .switchIfEmpty(Mono.error(ClientError.consentRequestNotFound()));
    }

    public Mono<ConsentArtefactRepresentation> getConsent(String consentId, String hiuId) {
        return consentArtefactRepository.getConsentArtefact(consentId)
                .switchIfEmpty(Mono.error(ClientError.consentArtefactNotFound()))
                .flatMap(r -> {
                    if (!r.getConsentDetail().getHiu().getId().equals(hiuId)) {
                        return Mono.error(ClientError.consentArtefactForbidden());
                    }
                    return Mono.just(r);
                });
    }
}
