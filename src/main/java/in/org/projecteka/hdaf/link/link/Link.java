package in.org.projecteka.hdaf.link.link;

import in.org.projecteka.hdaf.clients.ClientRegistryClient;
import in.org.projecteka.hdaf.link.ClientError;
import in.org.projecteka.hdaf.link.HIPClient;
import in.org.projecteka.hdaf.link.discovery.model.Identifier;
import in.org.projecteka.hdaf.link.link.model.PatientLinkReferenceRequest;
import in.org.projecteka.hdaf.link.link.model.PatientLinkReferenceResponse;
import in.org.projecteka.hdaf.link.link.model.PatientLinkRequest;
import in.org.projecteka.hdaf.link.link.model.PatientLinkResponse;
import in.org.projecteka.hdaf.link.link.model.hip.Patient;
import in.org.projecteka.hdaf.link.link.repository.LinkRepository;
import lombok.SneakyThrows;
import reactor.core.publisher.Mono;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

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
        return isOTPExpired(linkRefNumber).flatMap(expiry -> {
            if(!expiry){
                return linkRepository.getTransactionIdFromLinkReference(linkRefNumber)
                        .flatMap(linkRepository::getHIPIdFromDiscovery)
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
                return Mono.error(ClientError.otpExpired());
        });
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

    private Mono<Boolean> isOTPExpired(String linkRefNumber){
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
}
