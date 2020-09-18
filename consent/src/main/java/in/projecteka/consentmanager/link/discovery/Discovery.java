package in.projecteka.consentmanager.link.discovery;

import in.projecteka.consentmanager.clients.DiscoveryServiceClient;
import in.projecteka.consentmanager.clients.model.CareContextRepresentation;
import in.projecteka.consentmanager.link.discovery.model.patient.request.Patient;
import in.projecteka.consentmanager.link.discovery.model.patient.request.PatientIdentifier;
import in.projecteka.consentmanager.link.discovery.model.patient.request.PatientRequest;
import in.projecteka.consentmanager.link.discovery.model.patient.response.CareContext;
import in.projecteka.consentmanager.link.discovery.model.patient.response.DiscoveryResponse;
import in.projecteka.consentmanager.link.discovery.model.patient.response.DiscoveryResult;
import in.projecteka.consentmanager.link.link.LinkRepository;
import in.projecteka.consentmanager.properties.LinkServiceProperties;
import in.projecteka.library.clients.ErrorMap;
import in.projecteka.library.clients.UserServiceClient;
import in.projecteka.library.clients.model.ClientError;
import in.projecteka.library.clients.model.Error;
import in.projecteka.library.clients.model.ErrorCode;
import in.projecteka.library.clients.model.ErrorRepresentation;
import in.projecteka.library.clients.model.Identifier;
import in.projecteka.library.clients.model.Provider;
import in.projecteka.library.clients.model.RespError;
import in.projecteka.library.clients.model.User;
import in.projecteka.library.common.CentralRegistry;
import in.projecteka.library.common.DelayTimeoutException;
import in.projecteka.library.common.cache.CacheAdapter;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static in.projecteka.library.clients.model.ClientError.gatewayTimeOut;
import static in.projecteka.library.clients.model.ClientError.invalidResponseFromHIP;
import static in.projecteka.library.clients.model.ClientError.requestAlreadyExists;
import static in.projecteka.library.common.CustomScheduler.scheduleThis;
import static in.projecteka.library.common.Serializer.from;
import static in.projecteka.library.common.Serializer.tryTo;
import static java.time.Duration.ofMillis;
import static reactor.core.publisher.Mono.defer;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.just;

@AllArgsConstructor
public class Discovery {
    private static final String MOBILE = "MOBILE";
    private static final Logger logger = LoggerFactory.getLogger(Discovery.class);
    private final UserServiceClient userServiceClient;
    private final DiscoveryServiceClient discoveryServiceClient;
    private final DiscoveryRepository discoveryRepository;
    private final CentralRegistry centralRegistry;
    private final LinkServiceProperties serviceProperties;
    private final CacheAdapter<String, String> discoveryResults;
    private final LinkRepository linkRepository;

    public Flux<ProviderRepresentation> providersFrom(String name) {
        return centralRegistry.providersOf(name)
                .filter(this::isValid)
                .map(Transformer::to);
    }

    public Mono<ProviderRepresentation> providerBy(String id) {
        return centralRegistry.providerWith(id)
                .filter(this::isValid)
                .map(Transformer::to);
    }

    public Mono<DiscoveryResponse> patientInHIP(String userName,
                                                List<PatientIdentifier> unverifiedIdentifiers,
                                                String providerId,
                                                UUID transactionId,
                                                UUID requestId) {
        return just(requestId)
                .filterWhen(this::validateRequest)
                .switchIfEmpty(error(requestAlreadyExists()))
                .flatMap(val -> userWith(userName))
                .flatMap(user -> scheduleThis(discoveryServiceClient.requestPatientFor(
                        requestFor(user, transactionId, unverifiedIdentifiers, requestId),
                        providerId))
                        .timeout(ofMillis(getExpectedFlowResponseDuration()))
                        .responseFrom(discard -> defer(() -> discoveryResults.get(requestId.toString())
                                .zipWith(just(user.getIdentifier())))))
                .onErrorResume(DelayTimeoutException.class, discard -> error(gatewayTimeOut()))
                .flatMap(response -> tryTo(response.getT1(), DiscoveryResult.class)
                        .map(result -> just(result).zipWith(just(response.getT2())))
                        .orElse(error(invalidResponseFromHIP())))
                .switchIfEmpty(error(invalidResponseFromHIP()))
                .flatMap(discoveryResultUserIdTuple -> {
                    if (discoveryResultUserIdTuple.getT1().getError() != null) {
                        logger.error("[Discovery] Patient care-contexts discovery resulted in error {}",
                                discoveryResultUserIdTuple.getT1().getError());
                        return error(new ClientError(HttpStatus.NOT_FOUND,
                                cmErrorRepresentation(discoveryResultUserIdTuple.getT1().getError())));
                    }
                    if (discoveryResultUserIdTuple.getT1().getPatient() == null) {
                        logger.error("[Discovery] Patient care-contexts discovery should have returned in " +
                                "Patient reference with care context details or error caused." +
                                "Gateway requestId {}", discoveryResultUserIdTuple.getT1().getRequestId());
                        return error(invalidResponseFromHIP());
                    }
                    return ignoreLinkedCareContexts(discoveryResultUserIdTuple.getT2(),
                            discoveryResultUserIdTuple.getT1().getPatient().getCareContexts())
                            .flatMap(nonLinkedCareContexts -> {
                                var patientResult = discoveryResultUserIdTuple.getT1().getPatient();
                                var patient = in.projecteka.consentmanager.link.discovery.model.patient.response.Patient.builder()
                                        .referenceNumber(patientResult.getReferenceNumber())
                                        .display(patientResult.getDisplay())
                                        .careContexts(nonLinkedCareContexts)
                                        .matchedBy(patientResult.getMatchedBy())
                                        .build();
                                var discoveryResponse = DiscoveryResponse.builder()
                                        .patient(patient)
                                        .transactionId(transactionId)
                                        .build();
                                return just(discoveryResponse);
                            });
                })
                .doOnSuccess(r -> discoveryRepository
                        .insert(providerId, userName, transactionId, requestId)
                        .subscribe());
    }

    private Mono<List<CareContext>> ignoreLinkedCareContexts(String userId, List<CareContext> careContexts) {
        return linkRepository.getCareContextsForAUserId(userId)
                .flatMap(linkedCareContexts -> {
                    List<CareContext> filteredCareContexts = careContexts.stream().filter(careContext -> !filter(linkedCareContexts, careContext))
                            .collect(Collectors.toList());
                    return just(filteredCareContexts);
                });
    }

    private boolean filter(List<CareContextRepresentation> linkedCareContexts, CareContext careContext) {
        return linkedCareContexts.stream()
                .anyMatch(linkedCareContext ->
                        linkedCareContext.getReferenceNumber().equals(careContext.getReferenceNumber()));
    }

    private ErrorRepresentation cmErrorRepresentation(RespError respError) {
        Error error = Error.builder()
                .code(ErrorMap.toCmError(respError.getCode()))
                .message(respError.getMessage())
                .build();
        return ErrorRepresentation.builder().error(error).build();
    }

    public Mono<Void> onDiscoverPatientCareContexts(DiscoveryResult discoveryResult) {
        if (discoveryResult.getPatient() == null) {
            logger.error("[Discovery] Received a discovery response from Gateway without patient details for requestId.{} with error {}",
                    discoveryResult.getRequestId(), getDiscoveryError(discoveryResult));
            return handleDiscoveryError(discoveryResult.getResp().getRequestId(), "Patient Details not found");
        }

        if (StringUtils.isEmpty(discoveryResult.getPatient().getReferenceNumber())) {
            logger.error("[Discovery] Received a discovery response from Gateway without blank patient reference for requestId.{} with error {}",
                    discoveryResult.getRequestId(), getDiscoveryError(discoveryResult));
            return handleDiscoveryError(discoveryResult.getResp().getRequestId(), "Patient Reference should not be blank");
        }

        if (discoveryResult.getPatient().getCareContexts() == null) {
            logger.error("[Discovery] Received a discovery response from Gateway with care contexts as null for requestId.{}  with error {}",
                    discoveryResult.getRequestId(), getDiscoveryError(discoveryResult));
            return handleDiscoveryError(discoveryResult.getResp().getRequestId(), "Care contexts should not be null");
        }

        if (hasEmptyCareContextReferences(discoveryResult)) {
            logger.error("[Discovery] Received a discovery response from Gateway with invalid care context references for requestId.{} with error {}",
                    discoveryResult.getRequestId(), getDiscoveryError(discoveryResult));
            return handleDiscoveryError(discoveryResult.getResp().getRequestId(), "All the care contexts should have valid references");
        }

        if (discoveryResult.hasResponseId()) {
            return discoveryResults.put(discoveryResult.getResp().getRequestId(), from(discoveryResult));
        }
        logger.error("[Discovery] Received a discovery response from Gateway without original request Id mentioned.{}",
                discoveryResult.getRequestId());
        return error(ClientError.unprocessableEntity());
    }

    private String getDiscoveryError(DiscoveryResult discoveryResult) {
        return discoveryResult.getError() != null ? discoveryResult.getError().getMessage() : "";
    }

    private Mono<Void> handleDiscoveryError(String requestId, String errorMessage) {
        DiscoveryResult errorDiscovery = DiscoveryResult.builder()
                .error(RespError.builder()
                        .code(ErrorCode.INVALID_DISCOVERY.getValue())
                        .message("Could not find the user details")
                        .build())
                .build();

        return discoveryResults
                .put(requestId, from(errorDiscovery))
                .then(error(ClientError.invalidDiscovery(errorMessage)));
    }

    private boolean hasEmptyCareContextReferences(DiscoveryResult discoveryResult) {
        return discoveryResult.getPatient().getCareContexts().stream()
                .anyMatch(
                        careContext -> StringUtils.isEmpty(careContext.getReferenceNumber())
                );
    }

    private long getExpectedFlowResponseDuration() {
        return serviceProperties.getTxnTimeout();
    }

    private Mono<Boolean> validateRequest(UUID requestId) {
        return discoveryRepository.getIfPresent(requestId)
                .map(Objects::isNull)
                .switchIfEmpty(just(true));
    }

    private Mono<User> userWith(String patientId) {
        return userServiceClient.userOf(patientId);
    }

    private PatientRequest requestFor(User user,
                                      UUID transactionId,
                                      List<PatientIdentifier> unverifiedIdentifiers,
                                      UUID requestId) {
        var phoneNumber = in.projecteka.consentmanager.link.discovery.model.patient.request.Identifier.builder()
                .type(MOBILE)
                .value(user.getPhone())
                .build();
        List<in.projecteka.consentmanager.link.discovery.model.patient.request.Identifier> unverifiedIds =
                (unverifiedIdentifiers == null || unverifiedIdentifiers.isEmpty())
                ? Collections.emptyList()
                : unverifiedIdentifiers.stream().map(patientIdentifier ->
                        in.projecteka.consentmanager.link.discovery.model.patient.request.Identifier.builder()
                                .type(patientIdentifier.getType().toString())
                                .value(patientIdentifier.getValue())
                                .build()).collect(Collectors.toList());
        Patient patient = Patient.builder()
                .id(user.getIdentifier())
                .name(user.getName().createFullName())
                .gender(user.getGender())
                .yearOfBirth(user.getDateOfBirth().getYear())
                .verifiedIdentifiers(List.of(phoneNumber))
                .unverifiedIdentifiers(unverifiedIds)
                .build();
        return PatientRequest.builder()
                .patient(patient)
                .requestId(requestId)
                .transactionId(transactionId)
                .timestamp(LocalDateTime.now(ZoneOffset.UTC))
                .build();
    }

    private boolean isValid(Provider provider) {
        return provider.getIdentifiers().stream().anyMatch(Identifier::isOfficial);
    }
}
