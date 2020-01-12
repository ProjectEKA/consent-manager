package in.org.projecteka.hdaf.link.link;

import in.org.projecteka.hdaf.clients.ClientRegistryClient;
import in.org.projecteka.hdaf.link.ClientError;
import in.org.projecteka.hdaf.link.HIPClient;
import in.org.projecteka.hdaf.link.link.model.PatientLinkReferenceRequest;
import in.org.projecteka.hdaf.link.link.model.PatientLinkReferenceResponse;
import in.org.projecteka.hdaf.link.link.model.PatientLinkRequest;
import in.org.projecteka.hdaf.link.link.model.PatientLinkResponse;
import in.org.projecteka.hdaf.link.link.model.hip.Patient;
import in.org.projecteka.hdaf.link.link.repository.LinkRepository;
import reactor.core.publisher.Mono;

import static in.org.projecteka.hdaf.link.link.Transformer.toHIPPatient;

public class Link {
    private final HIPClient hipClient;
    private final ClientRegistryClient clientRegistryClient;
    private final LinkRepository linkRepository;

    public Link(HIPClient hipClient, ClientRegistryClient clientRegistryClient, LinkRepository linkRepository) {
        this.hipClient = hipClient;
        this.clientRegistryClient = clientRegistryClient;
        this.linkRepository = linkRepository;
    }

    public Mono<PatientLinkReferenceResponse> patientWith(String patientId, PatientLinkReferenceRequest patientLinkReferenceRequest) {
        Patient patient = toHIPPatient(patientId, patientLinkReferenceRequest.getPatient());
        var linkReferenceRequest = new in.org.projecteka.hdaf.link.link.model.hip.PatientLinkReferenceRequest(
                patientLinkReferenceRequest.getTransactionId(),
                patient);
        return linkRepository.getHIPIdFromDiscovery(patientLinkReferenceRequest.getTransactionId())
                .flatMap(hipId -> providerUrl(hipId)
                        .flatMap(url -> hipClient.linkPatientCareContext(linkReferenceRequest, url)
                                .flatMap(linkReferenceResponse -> {
                                    linkReferenceResponse.setTransactionId(patientLinkReferenceRequest.getTransactionId());
                                    return linkRepository.insertToLinkReference(
                                            linkReferenceResponse, hipId)
                                            .then(Mono.just(
                                                    PatientLinkReferenceResponse.builder()
                                                            .transactionId(
                                                                    linkReferenceResponse.getTransactionId())
                                                            .link(linkReferenceResponse.getLink()).build()));
                                })
                        )).switchIfEmpty(Mono.error(ClientError.unableToConnectToProvider()));
    }

    public Mono<PatientLinkResponse> verifyToken(String linkRefNumber, PatientLinkRequest patientLinkRequest, String patientId) {
        //Check otp for expiry
        return linkRepository.getTransactionIdFromLinkReference(linkRefNumber)
                .flatMap(transactionId -> linkRepository.getHIPIdFromDiscovery(transactionId))
                .flatMap(hipId -> providerUrl(hipId)
                        .flatMap(url -> hipClient.validateToken(linkRefNumber, patientLinkRequest, url)
                                .flatMap(patientLinkResponse -> linkRepository.insertToLink(
                                        hipId,
                                        patientId,
                                        linkRefNumber,
                                        patientLinkResponse.getPatient()
                                ).then(Mono.just(
                                        PatientLinkResponse.builder().patient(patientLinkResponse.getPatient()).build()))))
                        .switchIfEmpty(Mono.error(ClientError.unableToConnectToProvider())));
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
