package in.projecteka.consentmanager.link.link;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.LinkServiceClient;
import in.projecteka.consentmanager.clients.model.Error;
import in.projecteka.consentmanager.clients.model.ErrorRepresentation;
import in.projecteka.consentmanager.clients.model.Patient;
import in.projecteka.consentmanager.clients.model.PatientLinkReferenceResponse;
import in.projecteka.consentmanager.clients.model.PatientLinkReferenceResult;
import in.projecteka.consentmanager.clients.model.PatientLinkRequest;
import in.projecteka.consentmanager.clients.model.PatientLinkResponse;
import in.projecteka.consentmanager.clients.model.RespError;
import in.projecteka.consentmanager.clients.properties.LinkServiceProperties;
import in.projecteka.consentmanager.common.DelayTimeoutException;
import in.projecteka.consentmanager.common.ServiceAuthentication;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import in.projecteka.consentmanager.link.link.model.LinkConfirmationRequest;
import in.projecteka.consentmanager.link.link.model.LinkConfirmationResult;
import in.projecteka.consentmanager.link.link.model.PatientLinkReferenceRequest;
import in.projecteka.consentmanager.link.link.model.PatientLinksResponse;
import in.projecteka.consentmanager.link.link.model.TokenConfirmation;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

import static in.projecteka.consentmanager.clients.ErrorMap.toCmError;
import static in.projecteka.consentmanager.common.CustomScheduler.scheduleThis;
import static in.projecteka.consentmanager.common.Serializer.from;
import static in.projecteka.consentmanager.common.Serializer.tryTo;
import static in.projecteka.consentmanager.link.link.Transformer.toHIPPatient;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@AllArgsConstructor
public class Link {
    private static final Logger logger = LoggerFactory.getLogger(Link.class);
    private final LinkServiceClient linkServiceClient;
    private final LinkRepository linkRepository;
    private final ServiceAuthentication serviceAuthentication;
    private final LinkServiceProperties serviceProperties;
    private final CacheAdapter<String, String> linkResults;

    public Mono<PatientLinkReferenceResponse> patientCareContexts(
            String patientId,
            PatientLinkReferenceRequest patientLinkReferenceRequest) {
        Patient patient = toHIPPatient(patientId, patientLinkReferenceRequest.getPatient());
        var linkReferenceRequest = new in.projecteka.consentmanager.clients.model.PatientLinkReferenceRequest(
                patientLinkReferenceRequest.getRequestId().toString(),
                LocalDateTime.now(ZoneOffset.UTC),
                patientLinkReferenceRequest.getTransactionId(),
                patient);

        return Mono.just(patientLinkReferenceRequest.getRequestId())
                .filterWhen(this::validateRequest)
                .switchIfEmpty(Mono.error(ClientError.requestAlreadyExists()))
                .flatMap(id -> linkRepository.getHIPIdFromDiscovery(patientLinkReferenceRequest.getTransactionId())
                        .flatMap(hipId -> getHIPPatientLinkReferenceResponse(
                                linkReferenceRequest,
                                hipId,
                                patientLinkReferenceRequest.getRequestId())));
    }


    private Mono<Boolean> validateRequest(UUID requestId) {
        return linkRepository.selectLinkReference(requestId)
                .map(Objects::isNull)
                .switchIfEmpty(Mono.just(true));
    }

    private Mono<PatientLinkReferenceResponse> getHIPPatientLinkReferenceResponse(
            in.projecteka.consentmanager.clients.model.PatientLinkReferenceRequest linkReferenceRequest,
            String hipId,
            UUID requestId) {
        return serviceAuthentication.authenticate()
                .flatMap(token ->
                        scheduleThis(linkServiceClient.linkPatientEnquiryRequest(linkReferenceRequest, token, hipId))
                                .timeout(Duration.ofMillis(getExpectedFlowResponseDuration()))
                                .responseFrom(discard -> Mono.defer(() -> linkResults.get(requestId.toString())))
                                .onErrorResume(DelayTimeoutException.class, discard -> Mono.error(ClientError.gatewayTimeOut()))
                                .flatMap(response -> tryTo(response, PatientLinkReferenceResult.class).map(Mono::just).orElse(Mono.empty()))
                                .flatMap(linkReferenceResult -> {
                                    if (linkReferenceResult.getError() != null) {
                                        logger.error("[Link] Link initiation resulted in error {}", linkReferenceResult.getError());
                                        return Mono.error(new ClientError(BAD_REQUEST,
                                                cmErrorRepresentation(linkReferenceResult.getError())));
                                    }
                                    return linkRepository.insert(linkReferenceResult, hipId, requestId)
                                            .thenReturn(PatientLinkReferenceResponse.builder()
                                                    .transactionId(linkReferenceResult.getTransactionId().toString())
                                                    .link(linkReferenceResult.getLink())
                                                    .build());
                                }));
    }

    public Mono<PatientLinksResponse> getLinkedCareContexts(String patientId) {
        return linkRepository.getLinkedCareContextsForAllHip(patientId).map(patientLinks ->
                PatientLinksResponse.builder().patient(patientLinks).build());
    }

    public Mono<Void> onLinkCareContexts(PatientLinkReferenceResult patientLinkReferenceResult) {
        if (patientLinkReferenceResult.hasResponseId()) {
            return linkResults.put(patientLinkReferenceResult.getResp().getRequestId(),
                    from(patientLinkReferenceResult));
        }
        logger.error("[Link] Received a patient link reference response from Gateway without" +
                "original request Id mentioned.{}", patientLinkReferenceResult.getRequestId());
        return Mono.error(ClientError.unprocessableEntity());
    }

    public Mono<PatientLinkResponse> verifyLinkToken(String username, PatientLinkRequest patientLinkRequest) {
        UUID requestId = UUID.randomUUID();
        return linkRepository.getTransactionIdFromLinkReference(patientLinkRequest.getLinkRefNumber())
                .onErrorResume(error -> Mono.error(ClientError.invalidLinkReference()))
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
        return scheduleThis(linkServiceClient.confirmPatientLink(toLinkConfirmationRequest(patientLinkRequest, requestId), hipId))
                .timeout(Duration.ofMillis(getExpectedFlowResponseDuration()))
                .responseFrom(discard -> Mono.defer(() -> linkResults.get(requestId.toString())))
                .onErrorResume(DelayTimeoutException.class, discard -> Mono.error(ClientError.gatewayTimeOut()))
                .flatMap(response -> tryTo(response, LinkConfirmationResult.class).map(Mono::just).orElse(Mono.empty()))
                .flatMap(confirmationResult -> {
                    if (confirmationResult.getError() != null) {
                        logger.error("[Link] Link confirmation resulted in error {}", confirmationResult.getError());
                        return Mono.error(new ClientError(BAD_REQUEST, cmErrorRepresentation(confirmationResult.getError())));
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
        Error error = Error.builder().code(toCmError(respError.getCode())).message(respError.getMessage()).build();
        return ErrorRepresentation.builder().error(error).build();
    }

    private LinkConfirmationRequest toLinkConfirmationRequest(PatientLinkRequest patientLinkRequest, UUID requestId) {
        return LinkConfirmationRequest.builder()
                .requestId(requestId)
                .timestamp(LocalDateTime.now(ZoneOffset.UTC))
                .confirmation(new TokenConfirmation(patientLinkRequest.getLinkRefNumber(), patientLinkRequest.getToken()))
                .build();
    }

    private long getExpectedFlowResponseDuration() {
        return serviceProperties.getTxnTimeout();
    }

    public Mono<Void> onConfirmLink(LinkConfirmationResult confirmationResult) {
        if (confirmationResult.hasResponseId()) {
            return linkResults.put(confirmationResult.getResp().getRequestId(), from(confirmationResult));
        } else {
            logger.error("[Link] Received a confirmation response from Gateway " +
                    "without original request Id mentioned.{}", confirmationResult.getRequestId());
            return Mono.error(ClientError.unprocessableEntity());
        }
    }
}
