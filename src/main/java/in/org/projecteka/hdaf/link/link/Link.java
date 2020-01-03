package in.org.projecteka.hdaf.link.link;

import in.org.projecteka.hdaf.link.ClientRegistryClient;
import in.org.projecteka.hdaf.link.HIPClient;
import in.org.projecteka.hdaf.link.discovery.model.Identifier;
import in.org.projecteka.hdaf.link.link.model.PatientLinkReferenceRequest;
import in.org.projecteka.hdaf.link.link.model.PatientLinkReferenceResponse;
import reactor.core.publisher.Flux;

public class Link {

    private final HIPClient hipClient;
    private final ClientRegistryClient clientRegistryClient;

    public Link(HIPClient hipClient, ClientRegistryClient clientRegistryClient) {
        this.hipClient = hipClient;
        this.clientRegistryClient = clientRegistryClient;
    }

    public Flux<PatientLinkReferenceResponse> patientWith(String patientId, PatientLinkReferenceRequest patientLinkReferenceRequest) {
        //providerid to be fetched from DB using transactionID
        String providerId = "Max";
        return clientRegistryClient.providersOf(providerId)
                .map(provider -> provider.getIdentifiers()
                        .stream()
                        .findFirst()
                        .map(Identifier::getSystem))
                        .flatMap(s -> s.map(url -> hipClient.linkPatientCareContext(patientId,patientLinkReferenceRequest, url))
                                .orElse(Flux.error(new Throwable("Invalid HIP"))));
    }
}
