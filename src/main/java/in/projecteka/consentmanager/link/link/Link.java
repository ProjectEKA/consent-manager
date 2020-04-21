package in.projecteka.consentmanager.link.link;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.LinkServiceClient;
import in.projecteka.consentmanager.clients.model.Identifier;
import in.projecteka.consentmanager.clients.model.Patient;
import in.projecteka.consentmanager.clients.model.PatientLinkReferenceResponse;
import in.projecteka.consentmanager.clients.model.PatientLinkRequest;
import in.projecteka.consentmanager.clients.model.PatientLinkResponse;
import in.projecteka.consentmanager.clients.model.Provider;
import in.projecteka.consentmanager.common.CentralRegistry;
import in.projecteka.consentmanager.link.link.model.Hip;
import in.projecteka.consentmanager.link.link.model.Links;
import in.projecteka.consentmanager.link.link.model.PatientLinkReferenceRequest;
import in.projecteka.consentmanager.link.link.model.PatientLinks;
import in.projecteka.consentmanager.link.link.model.PatientLinksResponse;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static in.projecteka.consentmanager.link.link.Transformer.toHIPPatient;

@AllArgsConstructor
public class Link {
    private final LinkServiceClient linkServiceClient;
    private final LinkRepository linkRepository;
    private final CentralRegistry centralRegistry;

    public Mono<PatientLinkReferenceResponse> patientWith(String patientId,
                                                          PatientLinkReferenceRequest patientLinkReferenceRequest) {
        Patient patient = toHIPPatient(patientId, patientLinkReferenceRequest.getPatient());
        var linkReferenceRequest = new in.projecteka.consentmanager.clients.model.PatientLinkReferenceRequest(
                patientLinkReferenceRequest.getTransactionId(),
                patient);
        return linkRepository.getHIPIdFromDiscovery(patientLinkReferenceRequest.getTransactionId())
                .flatMap(hipId -> providerUrl(hipId)
                        .switchIfEmpty(Mono.error(ClientError.unableToConnectToProvider()))
                        .flatMap(url -> getPatientLinkReferenceResponse(patientLinkReferenceRequest,
                                linkReferenceRequest,
                                hipId,
                                url)));
    }

    private Mono<PatientLinkReferenceResponse> getPatientLinkReferenceResponse(
            PatientLinkReferenceRequest patientLinkReferenceRequest,
            in.projecteka.consentmanager.clients.model.PatientLinkReferenceRequest linkReferenceRequest,
            String hipId,
            String url) {
        return centralRegistry
                .authenticate()
                .flatMap(token -> linkServiceClient.linkPatientEnquiry(linkReferenceRequest, url, token))
                .flatMap(linkReferenceResponse -> {
                    linkReferenceResponse.setTransactionId(patientLinkReferenceRequest.getTransactionId());
                    return linkRepository.insertToLinkReference(linkReferenceResponse, hipId)
                            .thenReturn(PatientLinkReferenceResponse.builder()
                                    .transactionId(linkReferenceResponse.getTransactionId())
                                    .link(linkReferenceResponse.getLink()).build());
                });
    }

    public Mono<PatientLinkResponse> verifyToken(String linkRefNumber,
                                                 PatientLinkRequest patientLinkRequest,
                                                 String patientId) {
        return linkCareContexts(patientLinkRequest, linkRefNumber, patientId);
    }

    private Mono<PatientLinkResponse> linkCareContexts(PatientLinkRequest patientLinkRequest,
                                                       String linkRefNumber,
                                                       String patientId) {
        return linkRepository.getTransactionIdFromLinkReference(linkRefNumber)
                .flatMap(linkRepository::getHIPIdFromDiscovery)
                .flatMap(hipId -> providerUrl(hipId)
                        .switchIfEmpty(Mono.error(ClientError.unableToConnectToProvider()))
                        .flatMap(url -> getPatientLinkResponse(patientLinkRequest,
                                linkRefNumber,
                                patientId,
                                hipId,
                                url)));
    }

    private Mono<PatientLinkResponse> getPatientLinkResponse(
            PatientLinkRequest patientLinkRequest,
            String linkRefNumber,
            String patientId,
            String hipId,
            String url) {
        return centralRegistry.authenticate()
                .flatMap(token ->
                        linkServiceClient.linkPatientConfirmation(linkRefNumber, patientLinkRequest, url, token))
                .flatMap(patientLinkResponse ->
                        linkRepository.insertToLink(hipId, patientId, linkRefNumber, patientLinkResponse.getPatient())
                                .thenReturn(PatientLinkResponse.builder()
                                        .patient(patientLinkResponse.getPatient())
                                        .build()));
    }

    private Mono<String> providerUrl(String providerId) {
        return centralRegistry.providerWith(providerId)
                .flatMap(provider -> provider.getIdentifiers()
                        .stream()
                        .filter(Identifier::isOfficial)
                        .findFirst()
                        .map(identifier -> Mono.just(identifier.getSystem()))
                        .orElse(Mono.empty()));
    }

    public Mono<PatientLinksResponse> getLinkedCareContexts(String patientId) {
        return linkRepository.getLinkedCareContextsForAllHip(patientId)
                .flatMap(patientLinks -> getLinks(patientLinks.getLinks())
                        .map(links -> PatientLinksResponse.builder()
                                .patient(PatientLinks.builder()
                                        .id(patientLinks.getId())
                                        .links(links)
                                        .build())
                                .build()));
    }

    private Mono<List<Links>> getLinks(List<Links> patientLinks) {
        return Flux.fromIterable(patientLinks).flatMap(this::getHIPDetails).collectList();
    }

    private Mono<Links> getHIPDetails(Links links) {
        return getProviderName(links.getHip().getId())
                .map(name -> Links.builder()
                        .hip(Hip.builder().id(links.getHip().getId()).name(name).build())
                        .patientRepresentations(links.getPatientRepresentations())
                        .build());
    }

    private Mono<String> getProviderName(String providerId) {
        return centralRegistry.providerWith(providerId)
                .map(Provider::getName)
                .switchIfEmpty(Mono.empty());
    }
}
