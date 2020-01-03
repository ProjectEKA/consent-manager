package in.org.projecteka.hdaf.link.link;

import in.org.projecteka.hdaf.link.HIPClient;
import in.org.projecteka.hdaf.link.link.model.PatientLinkReferenceRequest;
import in.org.projecteka.hdaf.link.link.model.PatientLinkReferenceResponse;
import reactor.core.publisher.Flux;

public class LinkAccounts {

    private final HIPClient hipClient;

    public LinkAccounts(HIPClient hipClient) {
        this.hipClient = hipClient;
    }

    public Flux<PatientLinkReferenceResponse> linkAccounts(String authorization, PatientLinkReferenceRequest patientLinkReferenceRequest) {
        //providerid to be fetched from DB using transactionID
//        return clientRegistryClient.providersOf(providerId)
//                .map(provider -> provider.getIdentifiers()
//                        .stream()
//                        .filter(identifier -> identifier.getSystem().equals("http://localhost:8081"))
//                        .findFirst()
//                        .map(Identifier::getSystem))
//                .flatMap(s -> s.map(url ->
//                        hipServiceClient.patientFor(new PatientRequest("John", Arrays.asList(new in.org.projecteka.hdaf.link.discovery.model.patient.Identifier("Mobile", "9999999999"))), url))
//                        .orElse(Mono.error(new Throwable("Invalid HIP"))));
//

        return hipClient.linkPatientCareContext(authorization, patientLinkReferenceRequest);
    }
}
