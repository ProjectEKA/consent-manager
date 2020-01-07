package in.org.projecteka.hdaf.link.link;

import in.org.projecteka.hdaf.link.ClientRegistryClient;
import in.org.projecteka.hdaf.link.HIPClient;
import in.org.projecteka.hdaf.link.discovery.model.Identifier;
import in.org.projecteka.hdaf.link.link.model.PatientLinkReferenceRequest;
import in.org.projecteka.hdaf.link.link.model.PatientLinkReferenceResponse;
import in.org.projecteka.hdaf.link.link.model.PatientLinkRequest;
import in.org.projecteka.hdaf.link.link.model.PatientLinkResponse;
import reactor.core.publisher.Mono;

import java.util.Optional;

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

        return providerUrl(providerId)
                .flatMap(s -> s.map(url -> hipClient.linkPatientCareContext(patientId, patientLinkReferenceRequest, url))
                        .orElse(Mono.error(new Throwable("Invalid HIP")))
                );
    }

    public Mono<PatientLinkResponse> verifyToken(String patientId, String linkRefNumber, PatientLinkRequest patientLinkRequest) {
        //from linkRefNumber get TransactionId
        //from transactionID get providerID
        String providerId = "10000005";
        //Check otp for expiry
        return providerUrl(providerId)
                .flatMap(s -> s.map(url -> hipClient.validateToken(patientId, linkRefNumber, patientLinkRequest, url))
                        .orElse(Mono.error(new Throwable("Invalid HIP")))
                );
    }

    private Mono<Optional<String>> providerUrl(String providerId) {
        return clientRegistryClient.providerWith(providerId)
                .map(provider -> provider.getIdentifiers()
                        .stream()
                        .findFirst()
                        .map(Identifier::getSystem));
    }
}
