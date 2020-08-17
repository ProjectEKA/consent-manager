package in.projecteka.consentmanager.link.link;

import in.projecteka.consentmanager.clients.LinkServiceClient;
import in.projecteka.consentmanager.clients.model.Patient;
import in.projecteka.consentmanager.clients.model.PatientLinkReferenceResponse;
import in.projecteka.consentmanager.clients.model.PatientLinkReferenceResult;
import in.projecteka.consentmanager.clients.model.PatientLinkRequest;
import in.projecteka.consentmanager.clients.model.PatientLinkResponse;
import in.projecteka.consentmanager.link.Constants;
import in.projecteka.consentmanager.link.discovery.model.patient.response.GatewayResponse;
import in.projecteka.consentmanager.link.link.model.Acknowledgement;
import in.projecteka.consentmanager.link.link.model.AuthzHipAction;
import in.projecteka.consentmanager.link.link.model.LinkConfirmationRequest;
import in.projecteka.consentmanager.link.link.model.LinkConfirmationResult;
import in.projecteka.consentmanager.link.link.model.LinkRequest;
import in.projecteka.consentmanager.link.link.model.LinkResponse;
import in.projecteka.consentmanager.link.link.model.PatientLinkReferenceRequest;
import in.projecteka.consentmanager.link.link.model.PatientLinksResponse;
import in.projecteka.consentmanager.link.link.model.TokenConfirmation;
import in.projecteka.consentmanager.properties.LinkServiceProperties;
import in.projecteka.library.clients.model.ClientError;
import in.projecteka.library.clients.model.Error;
import in.projecteka.library.clients.model.ErrorRepresentation;
import in.projecteka.library.clients.model.RespError;
import in.projecteka.library.common.DelayTimeoutException;
import in.projecteka.library.common.ServiceAuthentication;
import in.projecteka.library.common.cache.CacheAdapter;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

import static in.projecteka.consentmanager.link.Constants.HIP_INITIATED_ACTION_LINK;
import static in.projecteka.consentmanager.link.link.Transformer.toHIPPatient;
import static in.projecteka.consentmanager.link.link.model.AcknowledgementStatus.SUCCESS;
import static in.projecteka.library.clients.ErrorMap.toCmError;
import static in.projecteka.library.clients.model.ClientError.invalidResponseFromHIP;
import static in.projecteka.library.clients.model.ErrorCode.TRANSACTION_ID_NOT_FOUND;
import static in.projecteka.library.common.CustomScheduler.scheduleThis;
import static in.projecteka.library.common.Serializer.from;
import static in.projecteka.library.common.Serializer.tryTo;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.error;

@AllArgsConstructor
public class Link {
    private static final Logger logger = LoggerFactory.getLogger(Link.class);
    private final LinkServiceClient linkServiceClient;
    private final LinkRepository linkRepository;
    private final ServiceAuthentication serviceAuthentication;
    private final LinkServiceProperties serviceProperties;
    private final CacheAdapter<String, String> linkResults;
    private final LinkTokenVerifier linkTokenVerifier;

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
                .switchIfEmpty(error(ClientError.requestAlreadyExists()))
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
                                .onErrorResume(DelayTimeoutException.class, discard -> error(ClientError.gatewayTimeOut()))
                                .flatMap(response -> tryTo(response, PatientLinkReferenceResult.class).map(Mono::just).orElse(empty()))
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
        return linkRepository.getLinkedCareContextsForAllHip(patientId)
                .map(patientLinks -> PatientLinksResponse.builder().patient(patientLinks).build());
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
                .onErrorResume(error -> {
                    if (error.getClass() == ClientError.class
                            && ((ClientError) error).getErrorCode().getValue() == TRANSACTION_ID_NOT_FOUND.getValue()) {
                        return Mono.error(ClientError.invalidLinkReference());
                    } else
                        return Mono.error(error);
                })
                .flatMap(linkRepository::getHIPIdFromDiscovery)
                .flatMap(hipId -> confirmAndLinkPatient(patientLinkRequest, username, hipId, requestId));
    }

    private Mono<PatientLinkResponse> confirmAndLinkPatient(
            PatientLinkRequest patientLinkRequest,
            String patientId,
            String hipId,
            UUID requestId) {
        return scheduleThis(
                linkServiceClient.confirmPatientLink(toLinkConfirmationRequest(patientLinkRequest, requestId), hipId))
                .timeout(Duration.ofMillis(getExpectedFlowResponseDuration()))
                .responseFrom(discard -> Mono.defer(() -> linkResults.get(requestId.toString())))
                .onErrorResume(DelayTimeoutException.class, discard -> error(ClientError.gatewayTimeOut()))
                .flatMap(response -> tryTo(response, LinkConfirmationResult.class).map(Mono::just).orElse(empty()))
                .flatMap(confirmationResult -> {
                    if (confirmationResult.getError() != null) {
                        logger.error("[Link] Link confirmation resulted in error {}", confirmationResult.getError());
                        return Mono.error(new ClientError(BAD_REQUEST,
                                cmErrorRepresentation(confirmationResult.getError())));
                    }
                    if (confirmationResult.getPatient() == null) {
                        logger.error("[Link] Link confirmation should have returned" +
                                "linked care context details or error caused. " +
                                "Gateway requestId {}", confirmationResult.getRequestId());
                        return error(invalidResponseFromHIP());
                    }
                    return linkRepository.insertToLink(hipId,
                            patientId,
                            patientLinkRequest.getLinkRefNumber(),
                            confirmationResult.getPatient(),
                            Constants.LINK_INITIATOR_CM)
                            .thenReturn(PatientLinkResponse.builder()
                                    .patient(confirmationResult.getPatient())
                                    .build());
                });
    }

    private ErrorRepresentation cmErrorRepresentation(RespError respError) {
        var error = Error.builder().code(toCmError(respError.getCode())).message(respError.getMessage()).build();
        return ErrorRepresentation.builder().error(error).build();
    }

    private LinkConfirmationRequest toLinkConfirmationRequest(PatientLinkRequest patientLinkRequest,
                                                              UUID requestId) {
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

    public Mono<Void> addCareContexts(LinkRequest linkRequest) {
        return linkTokenVerifier
                .getHipIdFromToken(linkRequest.getLink().getAccessToken())
                .flatMap(hipId ->
                        linkTokenVerifier.validateSession(linkRequest.getLink().getAccessToken())
                                .flatMap(hipAction -> linkTokenVerifier.validateHipAction(hipAction, HIP_INITIATED_ACTION_LINK))
                                .flatMap(hipAction -> linkRepository.insertToLink(
                                        hipAction.getHipId(),
                                        hipAction.getPatientId(),
                                        hipAction.getSessionId(),
                                        linkRequest.getLink().getPatient(),
                                        Constants.LINK_INITIATOR_HIP)
                                        .then(updateHipActionCounter(hipAction))
                                        .thenReturn(linkSuccessResponse(linkRequest)))
                                .onErrorResume(ClientError.class, exception -> linkFailureResponse(linkRequest, exception))
                                .flatMap(linkResponse -> linkServiceClient.sendLinkResponseToGateway(linkResponse, hipId)));
    }

    private Mono<Void> updateHipActionCounter(AuthzHipAction hipAction) {
        return linkRepository.incrementHipActionCounter(hipAction.getSessionId());
    }

    private Mono<? extends LinkResponse> linkFailureResponse(LinkRequest linkRequest, ClientError exception) {
        logger.error(exception.getError().getError().getMessage(), exception);
        var linkResponse = LinkResponse.builder()
                .requestId(UUID.randomUUID())
                .timestamp(LocalDateTime.now(ZoneOffset.UTC))
                .error(ClientError.from(exception))
                .resp(GatewayResponse.builder().requestId(linkRequest.getRequestId().toString()).build())
                .build();
        return Mono.just(linkResponse);
    }

    private LinkResponse linkSuccessResponse(LinkRequest linkRequest) {
        Acknowledgement acknowledgement = Acknowledgement.builder().status(SUCCESS).build();
        return LinkResponse.builder()
                .requestId(UUID.randomUUID())
                .timestamp(LocalDateTime.now(ZoneOffset.UTC))
                .acknowledgement(acknowledgement)
                .resp(GatewayResponse.builder().requestId(linkRequest.getRequestId().toString()).build())
                .build();
    }
}
