package in.projecteka.consentmanager.link.link;

import in.projecteka.consentmanager.clients.ClientRegistryClient;
import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.link.HIPClient;
import in.projecteka.consentmanager.clients.model.Identifier;
import in.projecteka.consentmanager.link.link.model.PatientLinkResponse;
import in.projecteka.consentmanager.link.link.model.PatientLinkReferenceRequest;
import in.projecteka.consentmanager.link.link.model.PatientLinkRequest;
import in.projecteka.consentmanager.link.link.model.PatientLinkReferenceResponse;
import in.projecteka.consentmanager.link.link.model.hip.Patient;
import reactor.core.publisher.Mono;

import static in.projecteka.consentmanager.link.link.Transformer.toHIPPatient;

import in.projecteka.consentmanager.link.link.repository.LinkRepository;
import lombok.SneakyThrows;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

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
        var linkReferenceRequest = new in.projecteka.consentmanager.link.link.model.hip.PatientLinkReferenceRequest(
                patientLinkReferenceRequest.getTransactionId(),
                patient);
        return linkRepository.getHIPIdFromDiscovery(patientLinkReferenceRequest.getTransactionId())
                .flatMap(hipId -> providerUrl(hipId)
                        .switchIfEmpty(Mono.error(ClientError.unableToConnectToProvider()))
                        .flatMap(url -> getPatientLinkReferenceResponse(patientLinkReferenceRequest, linkReferenceRequest, hipId, url)
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

    public Mono<PatientLinkResponse> verifyToken(String linkRefNumber, PatientLinkRequest patientLinkRequest, String patientId) {
        return isOTPExpired(linkRefNumber).flatMap(expiry -> {
            if (!expiry) {
                return linkCareContexts(patientLinkRequest, linkRefNumber, patientId);
            }
            return Mono.error(ClientError.otpExpired());
        });
    }

    private Mono<PatientLinkResponse> linkCareContexts(PatientLinkRequest patientLinkRequest, String linkRefNumber, String patientId) {
        return linkRepository.getTransactionIdFromLinkReference(linkRefNumber)
                .flatMap(linkRepository::getHIPIdFromDiscovery)
                .flatMap(hipId -> providerUrl(hipId)
                        .switchIfEmpty(Mono.error(ClientError.unableToConnectToProvider()))
                        .flatMap(url -> getPatientLinkResponse(patientLinkRequest, linkRefNumber, patientId, hipId, url)));
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
}
