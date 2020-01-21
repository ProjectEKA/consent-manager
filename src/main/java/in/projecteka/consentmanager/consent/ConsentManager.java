package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.clients.ClientRegistryClient;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.consent.model.request.ConsentDetail;
import in.projecteka.consentmanager.consent.repository.ConsentRequestRepository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.UUID;

public class ConsentManager {

    private final ClientRegistryClient providerClient;
    private UserServiceClient userServiceClient;
    private final ConsentRequestRepository repository;

    public ConsentManager(ConsentRequestRepository requestRepository, ClientRegistryClient providerClient, UserServiceClient userServiceClient) {
        this.repository =  requestRepository;
        this.providerClient = providerClient;
        this.userServiceClient = userServiceClient;
    }


    public Mono<String> askForConsent(String requestingHIUId, ConsentDetail consentDetail) {
        final String requestId = UUID.randomUUID().toString();
        return validatePatient(consentDetail).then(validateHIPAndHIU(consentDetail)).flatMap( result -> {
            return saveRequest(consentDetail, requestId);
        }).then(Mono.just(requestId));
//        return validProviders.then(saveRequest(consentDetail, requestId)).then(Mono.just(requestId));
    }

    private Mono<Boolean> validatePatient(ConsentDetail consentDetail) {
        return userServiceClient.userOf(consentDetail.getPatient().getId()).flatMap(p -> Mono.just(p != null));
    }

    private Mono<Boolean> validateHIPAndHIU(ConsentDetail consentDetail) {
        Mono<Boolean> checkHIU = isValidHIU(consentDetail).subscribeOn(Schedulers.elastic());
        Mono<Boolean> checkHIP = isValidHIP(consentDetail).subscribeOn(Schedulers.elastic());
        return Mono.zip(checkHIU, checkHIP, (validHIU, validHIP) -> validHIU & validHIP);
    }


    private Mono<Void> saveRequest(ConsentDetail consentDetail, String requestId) {
        return repository.insert(consentDetail, requestId);
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
}
