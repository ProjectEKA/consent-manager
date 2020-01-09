package in.org.projecteka.hdaf.link.link;

import in.org.projecteka.hdaf.link.ClientRegistryClient;
import in.org.projecteka.hdaf.link.HIPClient;
import in.org.projecteka.hdaf.link.link.model.*;
import in.org.projecteka.hdaf.link.link.model.hip.Patient;
import reactor.core.publisher.Mono;

import static in.org.projecteka.hdaf.link.link.Transformer.toHIPPatient;

public class Link {

    private final HIPClient hipClient;
    private final ClientRegistryClient clientRegistryClient;

    public Link(HIPClient hipClient, ClientRegistryClient clientRegistryClient) {
        this.hipClient = hipClient;
        this.clientRegistryClient = clientRegistryClient;
    }

    public Mono<PatientLinkReferenceResponse> patientWith(String patientId, PatientLinkReferenceRequest patientLinkReferenceRequest) {
        //providerid to be fetched from DB using transactionID
        String providerId = "10000005";
        Patient patient = toHIPPatient(patientId, patientLinkReferenceRequest);
        var linkReferenceRequest = new in.org.projecteka.hdaf.link.link.model.hip.PatientLinkReferenceRequest(
                patientLinkReferenceRequest.getTransactionId(),
                patient);
        return providerUrl(providerId)
                .flatMap(url -> hipClient.linkPatientCareContext(linkReferenceRequest, url))
                .switchIfEmpty(Mono.error(new Throwable("Invalid HIP")));
    }

    public Mono<PatientLinkResponse> verifyToken(String linkRefNumber, PatientLinkRequest patientLinkRequest) {
        //from linkRefNumber get TransactionId
        //from transactionID get providerID
        String providerId = "10000005";
        //Check otp for expiry
        return providerUrl(providerId)
                .flatMap(url -> hipClient.validateToken(linkRefNumber, patientLinkRequest, url))
                .switchIfEmpty(Mono.error(new Throwable("Invalid HIP")));
    }

    private Mono<String> providerUrl(String providerId) {
        return clientRegistryClient.providerWith(providerId)
                .flatMap(provider -> provider.getIdentifiers()
                        .stream()
                        .findFirst()
                        .map(identifier -> Mono.just(identifier.getSystem()))
                        .orElse(Mono.empty()));
    }
}
