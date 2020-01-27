package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.ClientRegistryClient;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.common.TokenUtils;
import in.projecteka.consentmanager.consent.model.ConsentArtefact;
import in.projecteka.consentmanager.consent.model.request.ConsentDetail;
import in.projecteka.consentmanager.consent.model.request.GrantedConsent;
import in.projecteka.consentmanager.consent.model.response.Consent;
import in.projecteka.consentmanager.consent.model.response.ConsentApprovalResponse;
import in.projecteka.consentmanager.consent.model.response.ConsentRequestDetail;
import in.projecteka.consentmanager.consent.model.response.ConsentStatus;
import in.projecteka.consentmanager.consent.repository.ConsentArtefactRepository;
import in.projecteka.consentmanager.consent.repository.ConsentRequestRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ConsentManager {

    private final ClientRegistryClient providerClient;
    private UserServiceClient userServiceClient;
    private final ConsentRequestRepository consentRequestRepository;
    private final ConsentArtefactRepository consentArtefactRepository;

    public ConsentManager(ConsentRequestRepository consentRequestRepository,
                          ClientRegistryClient providerClient,
                          UserServiceClient userServiceClient,
                          ConsentArtefactRepository consentArtefactRepository) {
        this.consentRequestRepository = consentRequestRepository;
        this.providerClient = providerClient;
        this.userServiceClient = userServiceClient;
        this.consentArtefactRepository = consentArtefactRepository;
    }

    public Mono<String> askForConsent(String requestingHIUId, ConsentDetail consentDetail) {
        final String requestId = UUID.randomUUID().toString();
        return validatePatient(consentDetail.getPatient().getId())
                .then(validateHIPAndHIU(consentDetail))
                .then(saveRequest(consentDetail, requestId))
                .thenReturn(requestId);
    }

    private Mono<Boolean> validatePatient(String patientId) {
        return userServiceClient.userOf(patientId).map(Objects::nonNull);
    }

    private Mono<Boolean> validateHIPAndHIU(ConsentDetail consentDetail) {
        Mono<Boolean> checkHIU = isValidHIU(consentDetail).subscribeOn(Schedulers.elastic());
        Mono<Boolean> checkHIP = isValidHIP(consentDetail).subscribeOn(Schedulers.elastic());
        return Mono.zip(checkHIU, checkHIP, (validHIU, validHIP) -> validHIU && validHIP);
    }


    private Mono<Void> saveRequest(ConsentDetail consentDetail, String requestId) {
        return consentRequestRepository.insert(consentDetail, requestId);
    }


    private Mono<Boolean> isValidHIU(ConsentDetail consentDetail) {
        return providerClient.providerWith(getHIUId(consentDetail)).flatMap(hiu -> Mono.just(hiu != null));
    }

    private Mono<Boolean> isValidHIP(ConsentDetail consentDetail) {
        String hipId = getHIPId(consentDetail);
        if (hipId != null) {
            return providerClient.providerWith(hipId).flatMap(hip -> Mono.just(hip != null));
        }
        return Mono.just(true);
    }

    private String getHIPId(ConsentDetail consentDetail) {
        if (consentDetail.getHip() != null) {
            return consentDetail.getHip().getId();
        }
        return null;
    }

    private String getHIUId(ConsentDetail consentDetail) {
        return consentDetail.getHiu().getId();
    }

    public Mono<List<ConsentRequestDetail>> findRequestsForPatient(String patientId, int limit, int offset) {
        return consentRequestRepository.requestsForPatient(patientId, limit, offset);
    }

    public Mono<ConsentApprovalResponse> approveConsent(String authorization,
                                                        String requestId,
                                                        List<GrantedConsent> grantedConsents) {
        String patientId = TokenUtils.readUserId(authorization);
        return validatePatient(patientId)
                .then(validateConsentRequest(requestId))
                .flatMap(consentRequest ->
                        Flux.fromIterable(grantedConsents)
                                .flatMap(grantedConsent -> {
                                    var consentArtefact = mapToConsentArtefact(consentRequest, grantedConsent);
                                    return consentArtefactRepository.insert(consentArtefact, requestId, patientId, "")
                                            .then(consentRequestRepository.updateStatus(requestId, ConsentStatus.GRANTED.toString()))
                                            .thenReturn(Consent.builder().id(consentArtefact.getId()).build());
                                }).collectList())
                .map(consents -> ConsentApprovalResponse.builder().consents(consents).build());
    }

    private ConsentArtefact mapToConsentArtefact(ConsentRequestDetail consentRequest, GrantedConsent consent) {
        String consentArtefactId = UUID.randomUUID().toString();
        return ConsentArtefact.builder()
                .id(consentArtefactId)
                .requestId(consentRequest.getRequestId())
                .createdAt(consentRequest.getCreatedAt())
                .purpose(consentRequest.getPurpose())
                .patient(consentRequest.getPatient())
                .hiu(consentRequest.getHiu())
                .requester(consentRequest.getRequester())
                .hip(consent.getHip())
                .hiTypes(consent.getHiTypes())
                .permission(consent.getPermission())
                .build();
    }

    private Mono<ConsentRequestDetail> validateConsentRequest(String requestId) {
        return consentRequestRepository.requestOf(requestId, ConsentStatus.REQUESTED.toString())
                .switchIfEmpty(Mono.error(ClientError.consentRequestNotFound()));
    }
}
