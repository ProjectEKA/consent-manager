package in.projecteka.hdaf.link.link;

import in.projecteka.hdaf.clients.ClientRegistryClient;
import in.projecteka.hdaf.link.ClientError;
import in.projecteka.hdaf.link.HIPClient;
import in.projecteka.hdaf.link.link.model.PatientLinkResponse;
import in.projecteka.hdaf.link.link.model.PatientLinkReferenceRequest;
import in.projecteka.hdaf.link.link.model.PatientLinkRequest;
import in.projecteka.hdaf.link.link.model.PatientLinkReferenceResponse;
import in.projecteka.hdaf.link.link.model.hip.Patient;
import reactor.core.publisher.Mono;

import static in.projecteka.hdaf.link.link.Transformer.toHIPPatient;

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
        Patient patient = toHIPPatient(patientId, patientLinkReferenceRequest.getPatient());
        var linkReferenceRequest = new in.projecteka.hdaf.link.link.model.hip.PatientLinkReferenceRequest(
                patientLinkReferenceRequest.getTransactionId(),
                patient);
        return providerUrl(providerId)
                .flatMap(url -> hipClient.linkPatientCareContext(linkReferenceRequest, url))
                .switchIfEmpty(Mono.error(ClientError.unableToConnectToProvider()));
    }

    public Mono<PatientLinkResponse> verifyToken(String linkRefNumber, PatientLinkRequest patientLinkRequest) {
        //from linkRefNumber get TransactionId
        //from transactionID get providerID
        String providerId = "10000005";
        //Check otp for expiry
        return providerUrl(providerId)
                .flatMap(url -> hipClient.validateToken(linkRefNumber, patientLinkRequest, url))
                .switchIfEmpty(Mono.error(ClientError.unableToConnectToProvider()));
    }

    private Mono<String> providerUrl(String providerId) {
        return clientRegistryClient.providerWith(providerId)
                .flatMap(provider -> provider.getIdentifiers()
                        .stream()
                        .filter(identifier -> identifier.isOfficial())
                        .findFirst()
                        .map(identifier -> Mono.just(identifier.getSystem()))
                        .orElse(Mono.empty()));
    }
}
