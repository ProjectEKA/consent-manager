package in.projecteka.consentmanager.link.link;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.ClientRegistryClient;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.clients.model.Identifier;
import in.projecteka.consentmanager.clients.model.User;
import in.projecteka.consentmanager.link.HIPClient;
import in.projecteka.consentmanager.link.link.model.Hip;
import in.projecteka.consentmanager.link.link.model.Links;
import in.projecteka.consentmanager.link.link.model.PatientLinkReferenceRequest;
import in.projecteka.consentmanager.link.link.model.PatientLinkReferenceResponse;
import in.projecteka.consentmanager.link.link.model.PatientLinkRequest;
import in.projecteka.consentmanager.link.link.model.PatientLinkResponse;
import in.projecteka.consentmanager.link.link.model.PatientLinks;
import in.projecteka.consentmanager.link.link.model.PatientLinksResponse;
import in.projecteka.consentmanager.link.link.model.hip.Patient;
import lombok.SneakyThrows;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static in.projecteka.consentmanager.link.link.Transformer.toHIPPatient;

public class Link {
    private final HIPClient hipClient;
    private final ClientRegistryClient clientRegistryClient;
    private final LinkRepository linkRepository;
    private UserServiceClient userServiceClient;

    public Link(HIPClient hipClient, ClientRegistryClient clientRegistryClient, LinkRepository linkRepository,
                UserServiceClient userServiceClient) {
        this.hipClient = hipClient;
        this.clientRegistryClient = clientRegistryClient;
        this.linkRepository = linkRepository;
        this.userServiceClient = userServiceClient;
    }

    public Mono<PatientLinkReferenceResponse> patientWith(String patientId,
                                                          PatientLinkReferenceRequest patientLinkReferenceRequest) {
        Patient patient = toHIPPatient(patientId, patientLinkReferenceRequest.getPatient());
        var linkReferenceRequest = new in.projecteka.consentmanager.link.link.model.hip.PatientLinkReferenceRequest(
                patientLinkReferenceRequest.getTransactionId(),
                patient);
        return linkRepository.getHIPIdFromDiscovery(patientLinkReferenceRequest.getTransactionId())
                .flatMap(hipId -> providerUrl(hipId)
                        .switchIfEmpty(Mono.error(ClientError.unableToConnectToProvider()))
                        .flatMap(url -> getPatientLinkReferenceResponse(patientLinkReferenceRequest,
                                linkReferenceRequest, hipId, url)
                        ));
    }

    private Mono<PatientLinkReferenceResponse> getPatientLinkReferenceResponse(
            PatientLinkReferenceRequest patientLinkReferenceRequest,
            in.projecteka.consentmanager.link.link.model.hip.PatientLinkReferenceRequest linkReferenceRequest,
            String hipId,
            String url) {
        return hipClient.linkPatientCareContext(linkReferenceRequest, url)
                .flatMap(linkReferenceResponse -> {
                    linkReferenceResponse.setTransactionId(patientLinkReferenceRequest.getTransactionId());
                    return linkRepository.insertToLinkReference(
                            linkReferenceResponse, hipId)
                            .then(Mono.just(
                                    PatientLinkReferenceResponse.builder()
                                            .transactionId(
                                                    linkReferenceResponse.getTransactionId())
                                            .link(linkReferenceResponse.getLink()).build()));
                });
    }

    public Mono<PatientLinkResponse> verifyToken(String linkRefNumber, PatientLinkRequest patientLinkRequest,
                                                 String patientId) {
        return isOTPExpired(linkRefNumber).flatMap(expiry -> {
            if (!expiry) {
                return linkCareContexts(patientLinkRequest, linkRefNumber, patientId);
            }
            return Mono.error(ClientError.otpExpired());
        });
    }

    private Mono<PatientLinkResponse> linkCareContexts(PatientLinkRequest patientLinkRequest, String linkRefNumber,
                                                       String patientId) {
        return linkRepository.getTransactionIdFromLinkReference(linkRefNumber)
                .flatMap(linkRepository::getHIPIdFromDiscovery)
                .flatMap(hipId -> providerUrl(hipId)
                        .switchIfEmpty(Mono.error(ClientError.unableToConnectToProvider()))
                        .flatMap(url -> getPatientLinkResponse(patientLinkRequest, linkRefNumber, patientId, hipId,
                                url)));
    }

    private Mono<PatientLinkResponse> getPatientLinkResponse(
            PatientLinkRequest patientLinkRequest,
            String linkRefNumber,
            String patientId,
            String hipId,
            String url) {
        return hipClient.validateToken(linkRefNumber, patientLinkRequest, url)
                .flatMap(patientLinkResponse -> linkRepository.insertToLink(
                        hipId,
                        patientId,
                        linkRefNumber,
                        patientLinkResponse.getPatient()
                ).then(Mono.just(
                        PatientLinkResponse.builder().patient(patientLinkResponse.getPatient()).build())));
    }

    private Mono<String> providerUrl(String providerId) {
        return clientRegistryClient.providerWith(providerId)
                .flatMap(provider -> provider.getIdentifiers()
                        .stream()
                        .filter(Identifier::isOfficial)
                        .findFirst()
                        .map(identifier -> Mono.just(identifier.getSystem()))
                        .orElse(Mono.empty()));
    }

    private Mono<Boolean> isOTPExpired(String linkRefNumber) {
        return linkRepository.getExpiryFromLinkReference(linkRefNumber)
                .flatMap(this::isExpired)
                .switchIfEmpty(Mono.empty());
    }

    @SneakyThrows
    private Mono<Boolean> isExpired(String expiry) {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        df.setTimeZone(tz);
        Date date = df.parse(expiry);
        return Mono.just(date.before(new Date()));
    }

    public Mono<PatientLinksResponse> getLinkedCareContexts(String patientId) {
        return linkRepository.getLinkedCareContextsForAllHip(patientId)
                .flatMap(patientLinks -> getLinks(patientLinks.getLinks())
                        .flatMap(links -> userWith(patientId)
                                .flatMap(user -> Mono.just(PatientLinksResponse.builder().
                                        patient(PatientLinks.builder()
                                                .id(patientLinks.getId())
                                                .firstName(user.getFirstName())
                                                .lastName(user.getLastName())
                                                .links(links)
                                                .build())
                                .build()))));
    }

    private Mono<User> userWith(String patientId) {
        return userServiceClient.userOf(patientId);
    }

    private Mono<List<Links>> getLinks(List<Links> patientLinks) {
        Flux<Links> linksFlux = Flux.fromIterable(patientLinks).flatMap(this::getHIPDetails);
        return linksFlux.collectList();

    }

    private Mono<Links> getHIPDetails(Links links) {
        return getProviderName(links.getHip().getId())
                .flatMap(name ->
                        Mono.just(Links.builder()
                                .hip(Hip.builder().id(links.getHip().getId()).name(name).build())
                                .patientRepresentations(links.getPatientRepresentations())
                                .build()));
    }

    private Mono<String> getProviderName(String providerId) {
        return clientRegistryClient.providerWith(providerId)
                .flatMap(provider -> Mono.just(provider.getName()))
                .switchIfEmpty(Mono.empty());

    }
}
