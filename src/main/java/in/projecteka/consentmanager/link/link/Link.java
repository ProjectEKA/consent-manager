package in.projecteka.consentmanager.link.link;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.ErrorMap;
import in.projecteka.consentmanager.clients.LinkServiceClient;
import in.projecteka.consentmanager.clients.model.Error;
import in.projecteka.consentmanager.clients.model.ErrorRepresentation;
import in.projecteka.consentmanager.clients.model.Identifier;
import in.projecteka.consentmanager.clients.model.RespError;
import in.projecteka.consentmanager.link.link.model.LinkConfirmationRequest;
import in.projecteka.consentmanager.clients.model.Patient;
import in.projecteka.consentmanager.clients.model.PatientLinkReferenceResponse;
import in.projecteka.consentmanager.clients.model.PatientLinkReferenceResult;
import in.projecteka.consentmanager.clients.model.PatientLinkRequest;
import in.projecteka.consentmanager.clients.model.PatientLinkResponse;
import in.projecteka.consentmanager.link.link.model.LinkConfirmationResult;
import in.projecteka.consentmanager.link.link.model.TokenConfirmation;
import in.projecteka.consentmanager.clients.properties.LinkServiceProperties;
import in.projecteka.consentmanager.common.CentralRegistry;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import in.projecteka.consentmanager.link.link.model.PatientLinkReferenceRequest;
import in.projecteka.consentmanager.link.link.model.PatientLinksResponse;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import static in.projecteka.consentmanager.link.link.Transformer.toHIPPatient;

@AllArgsConstructor
public class Link {
    private static final Logger logger = LoggerFactory.getLogger(Link.class);
    private final LinkServiceClient linkServiceClient;
    private final LinkRepository linkRepository;
    private final CentralRegistry centralRegistry;
    private final LinkServiceProperties serviceProperties;
    private final CacheAdapter<String, String> linkResults;

    public Mono<PatientLinkReferenceResponse> patientWith(String patientId,
                                                          PatientLinkReferenceRequest patientLinkReferenceRequest) {
        Patient patient = toHIPPatient(patientId, patientLinkReferenceRequest.getPatient());
        var linkReferenceRequest = new in.projecteka.consentmanager.clients.model.PatientLinkReferenceRequest(
                patientLinkReferenceRequest.getRequestId().toString(),
                patientLinkReferenceRequest.getTransactionId(),
                patient);
        return Mono.just(patientLinkReferenceRequest.getRequestId())
                .filterWhen(this::validateRequest)
                .switchIfEmpty(Mono.error(ClientError.requestAlreadyExists()))
                .flatMap(id -> linkRepository.getHIPIdFromDiscovery(patientLinkReferenceRequest.getTransactionId())
                        .flatMap(hipId -> providerUrl(hipId)
                                .switchIfEmpty(Mono.error(ClientError.unableToConnectToProvider()))
                                .flatMap(url -> getPatientLinkReferenceResponse(patientLinkReferenceRequest,
                                        linkReferenceRequest,
                                        hipId,
                                        url))));
    }

    public Mono<PatientLinkReferenceResponse> patientCareContexts(String patientId,
                                                                  PatientLinkReferenceRequest patientLinkReferenceRequest) {
        Patient patient = toHIPPatient(patientId, patientLinkReferenceRequest.getPatient());
        var linkReferenceRequest = new in.projecteka.consentmanager.clients.model.PatientLinkReferenceRequest(
                patientLinkReferenceRequest.getRequestId().toString(),
                patientLinkReferenceRequest.getTransactionId(),
                patient);

        return Mono.just(patientLinkReferenceRequest.getRequestId())
                .filterWhen(this::validateRequest)
                .switchIfEmpty(Mono.error(ClientError.requestAlreadyExists()))
                .flatMap(id -> linkRepository.getHIPIdFromDiscovery(patientLinkReferenceRequest.getTransactionId())
                        .flatMap(hipId -> getHIPPatientLinkReferenceResponse(
                                linkReferenceRequest,
                                hipId,
                                patientLinkReferenceRequest.getRequestId()
                        ))
                );
    }


    private Mono<Boolean> validateRequest(UUID requestId) {
        return linkRepository.selectLinkReference(requestId)
                .map(Objects::isNull)
                .switchIfEmpty(Mono.just(true));
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
                    return linkRepository.insertToLinkReference(linkReferenceResponse,
                            hipId,
                            patientLinkReferenceRequest.getRequestId())
                            .thenReturn(PatientLinkReferenceResponse.builder()
                                    .transactionId(linkReferenceResponse.getTransactionId())
                                    .link(linkReferenceResponse.getLink()).build());
                });
    }

    private Mono<PatientLinkReferenceResponse> getHIPPatientLinkReferenceResponse(
            in.projecteka.consentmanager.clients.model.PatientLinkReferenceRequest linkReferenceRequest,
            String hipId,
            UUID requestId
    ) {
        return centralRegistry
                .authenticate()
                .flatMap(token -> linkServiceClient.linkPatientEnquiryRequest(linkReferenceRequest, token, hipId))
                .zipWith(Mono.delay(Duration.ofMillis(getExpectedFlowResponseDuration())))
                .flatMap(tuple ->
                        linkResults.get(requestId.toString())
                                .switchIfEmpty(Mono.error(ClientError.gatewayTimeOut()))
                                .flatMap(this::deserializeLinkReferenceResponseFromHIP))
                .switchIfEmpty(Mono.error(ClientError.networkServiceCallFailed()))
                .flatMap(linkReferenceResult -> {
                    if (linkReferenceResult.getError() != null) {
                        logger.error("[Link] Link initiation resulted in error {}", linkReferenceResult.getError());
                        return Mono.error(new ClientError(HttpStatus.BAD_REQUEST, cmErrorRepresentation(linkReferenceResult.getError())));
                    }
                    return linkRepository.insert(linkReferenceResult, hipId, requestId)
                            .thenReturn(((PatientLinkReferenceResponse.builder()
                                    .transactionId(linkReferenceResult.getTransactionId().toString())
                                    .link(linkReferenceResult.getLink())
                                    .build())));
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
        return linkRepository.getLinkedCareContextsForAllHip(patientId).map(patientLinks ->
                PatientLinksResponse.builder().patient(patientLinks).build());
    }

    public Mono<Void> onLinkCareContexts(PatientLinkReferenceResult patientLinkReferenceResult) {
        if(patientLinkReferenceResult.hasResponseId()) {
            return linkResults.put(patientLinkReferenceResult.getResp().getRequestId(), serializeLinkReferenceResultFromHIP(patientLinkReferenceResult));
        }
        logger.error("[Link] Received a patient link reference response from Gateway without original request Id mentioned.{}", patientLinkReferenceResult.getRequestId());
        return Mono.error(ClientError.unprocessableEntity());
    }

    private String serializeLinkReferenceResultFromHIP(PatientLinkReferenceResult responseBody) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(responseBody);
        } catch (JsonProcessingException e) {
            logger.error("[Link] Can not serialize patient link reference response from HIP", e);
        }
        return null;
    }

    public Mono<PatientLinkResponse> verifyLinkToken(String username, PatientLinkRequest patientLinkRequest) {
        UUID requestId = UUID.randomUUID();
        return linkRepository.getTransactionIdFromLinkReference(patientLinkRequest.getLinkRefNumber())
                .switchIfEmpty(Mono.error(ClientError.transactionIdNotFound()))
                .flatMap(linkRepository::getHIPIdFromDiscovery)
                .flatMap(hipId ->
                        confirmAndLinkPatient(patientLinkRequest,
                                username,
                                hipId,
                                requestId));
    }

    private Mono<PatientLinkResponse> confirmAndLinkPatient(
            PatientLinkRequest patientLinkRequest,
            String patientId,
            String hipId,
            UUID requestId) {
        return linkServiceClient.confirmPatientLink(toLinkConfirmationRequest(patientLinkRequest, requestId), hipId)
                .zipWith(Mono.delay(Duration.ofMillis(getExpectedFlowResponseDuration())))
                .flatMap(tuple ->
                        linkResults.get(requestId.toString())
                                .switchIfEmpty(Mono.error(ClientError.gatewayTimeOut()))
                                .flatMap(lcr -> deserializeConfirmationFromHIP(lcr)))
                .switchIfEmpty(Mono.error(ClientError.networkServiceCallFailed()))
                .flatMap(confirmationResult -> {
                    if (confirmationResult.getError() != null) {
                        logger.error("[Link] Link confirmation resulted in error {}", confirmationResult.getError());
                        return Mono.error(new ClientError(HttpStatus.BAD_REQUEST, cmErrorRepresentation(confirmationResult.getError())));
                    }
                    if (confirmationResult.getPatient() == null) {
                        logger.error("[Link] Link confirmation should have returned linked care context details or error caused." +
                                "Gateway requestId {}", confirmationResult.getRequestId());
                        return Mono.error(ClientError.invalidResponseFromHIP());
                    }
                    return linkRepository.insertToLink(hipId, patientId, patientLinkRequest.getLinkRefNumber(), confirmationResult.getPatient())
                            .thenReturn(PatientLinkResponse.builder()
                                    .patient(confirmationResult.getPatient())
                                    .build());
                });
    }

    private ErrorRepresentation cmErrorRepresentation(RespError respError) {
        Error error = Error.builder().code(ErrorMap.hipToCmError(respError.getCode())).message(respError.getMessage()).build();
        return ErrorRepresentation.builder().error(error).build();
    }

    private LinkConfirmationRequest toLinkConfirmationRequest(PatientLinkRequest patientLinkRequest, UUID requestId) {
        return LinkConfirmationRequest.builder()
                    .requestId(requestId)
                    .timestamp(Instant.now().toString())
                    .confirmation(new TokenConfirmation(patientLinkRequest.getLinkRefNumber(), patientLinkRequest.getToken()))
                    .build();
    }

    private long getExpectedFlowResponseDuration() {
        return serviceProperties.getTxnTimeout();
    }

    public Mono<Void> onConfirmLink(LinkConfirmationResult confirmationResult) {
        if(confirmationResult.hasResponseId()) {
            return linkResults.put(confirmationResult.getResp().getRequestId(), serializeConfirmationFromHIP(confirmationResult));
        } else {
            logger.error("[Link] Received a confirmation response from Gateway without original request Id mentioned.{}", confirmationResult.getRequestId());
            return Mono.error(ClientError.unprocessableEntity());
        }
    }

    private String serializeConfirmationFromHIP(LinkConfirmationResult confirmationResult) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(confirmationResult);
        } catch (JsonProcessingException e) {
            logger.error("[Link] Can not serialize response from HIP", e);
        }
        return null;
    }

    private Mono<LinkConfirmationResult> deserializeConfirmationFromHIP(String responseBody) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return Mono.just(objectMapper.readValue(responseBody, LinkConfirmationResult.class));
        } catch (JsonProcessingException e) {
            logger.error("[Link] Can not deserialize response from HIP", e);
        }
        return Mono.empty();
    }

    private Mono<PatientLinkReferenceResult> deserializeLinkReferenceResponseFromHIP(String responseBody) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return Mono.just(objectMapper.readValue(responseBody, PatientLinkReferenceResult.class));
        } catch (JsonProcessingException e) {
            logger.error("[Link] Can not deserialize link reference response from HIP", e);
        }
        return Mono.empty();
    }
}
