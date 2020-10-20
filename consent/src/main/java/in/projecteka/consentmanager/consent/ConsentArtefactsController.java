package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.consent.model.ConsentArtefactsStatusResponse;
import in.projecteka.consentmanager.consent.model.FetchRequest;
import in.projecteka.consentmanager.consent.model.RevokeRequest;
import in.projecteka.consentmanager.consent.model.request.ConsentRequestStatus;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactLightRepresentation;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactRepresentation;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactResponse;
import in.projecteka.consentmanager.consent.model.response.HIPConsentNotificationAcknowledgment;
import in.projecteka.consentmanager.consent.model.response.HIUConsentNotificationAcknowledgment;
import in.projecteka.library.clients.model.ClientError;
import in.projecteka.library.common.Caller;
import in.projecteka.library.common.RequestValidator;
import in.projecteka.library.common.ServiceCaller;
import in.projecteka.library.common.cache.CacheAdapter;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.Optional;
import java.util.UUID;

import static in.projecteka.consentmanager.consent.Constants.CONSENT_REQUESTS_STATUS;
import static in.projecteka.consentmanager.consent.Constants.PATH_CONSENTS_FETCH;
import static in.projecteka.consentmanager.consent.Constants.PATH_HIP_CONSENT_ON_NOTIFY;
import static in.projecteka.consentmanager.consent.Constants.PATH_HIU_CONSENT_ON_NOTIFY;
import static in.projecteka.consentmanager.consent.model.HipConsentArtefactNotificationStatus.NOTIFIED;
import static in.projecteka.library.common.Constants.CORRELATION_ID;

@RestController
@AllArgsConstructor
public class ConsentArtefactsController {

    private static final Logger logger = LoggerFactory.getLogger(ConsentArtefactsController.class);
    private final ConsentManager consentManager;
    private final CacheAdapter<String, String> usedTokens;
    private final ConsentServiceProperties serviceProperties;
    private final RequestValidator validator;
    private final CacheAdapter<String, String> hipConsentArtefactStatus;

    @GetMapping(value = Constants.APP_PATH_GET_CONSENT)
    public Mono<ConsentArtefactRepresentation> getConsentArtefact(@PathVariable(value = "consentId") String consentId) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (ServiceCaller) securityContext.getAuthentication().getPrincipal())
                .flatMap(requester -> consentManager.getConsent(consentId, requester.getClientId()));
    }

    @GetMapping(value = Constants.APP_PATH_INTERNAL_GET_CONSENT)
    public Mono<ConsentArtefactLightRepresentation> getConsent(@PathVariable String consentId) {
        return consentManager.getConsentArtefactLight(consentId);
    }

    @GetMapping(value = Constants.APP_PATH_GET_CONSENT_ARTEFACTS_FOR_REQUEST)
    public Flux<ConsentArtefactRepresentation> getConsents(@PathVariable(value = "request-id") String requestId) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getUsername)
                .flatMapMany(patient -> consentManager.getConsents(requestId, patient));
    }

    @GetMapping(value = Constants.APP_PATH_GET_CONSENT_ARTEFACTS)
    public Mono<ConsentArtefactResponse> getAllConsentArtefacts(
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "-1") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        int pageSize = getPageSize(limit);
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .flatMap(caller -> consentManager.getAllConsentArtefacts(caller.getUsername(), pageSize, offset, status))
                .map(artefacts -> ConsentArtefactResponse.builder()
                        .consentArtefacts(artefacts.getResult())
                        .size(artefacts.getTotal())
                        .limit(pageSize)
                        .offset(offset).build());
    }

    @PostMapping(value = Constants.APP_PATH_REVOKE_CONSENTS)
    public Mono<Void> revokeConsent(@RequestBody RevokeRequest revokeRequest) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .flatMap(caller -> consentManager.revoke(revokeRequest, caller.getUsername())
                        .switchIfEmpty(Mono.defer(() -> {
                            logger.debug("[revoke] putting {} in used tokens", caller.getSessionId());
                            return usedTokens.put(caller.getSessionId(), "");
                        }))
                );
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(value = PATH_CONSENTS_FETCH)
    public Mono<Void> fetchConsent(@RequestBody FetchRequest fetchRequest) {
        return Mono.just(fetchRequest)
                .filterWhen(req ->
                        validator.validate(fetchRequest.getRequestId().toString(), fetchRequest.getTimestamp()))
                .switchIfEmpty(Mono.error(ClientError.tooManyRequests()))
                .flatMap(validatedRequest -> ReactiveSecurityContextHolder.getContext()
                        .map(securityContext -> (ServiceCaller) securityContext.getAuthentication().getPrincipal())
                        .doOnSuccess(requester -> Mono.defer(() -> {
                            validator.put(fetchRequest.getRequestId().toString(), fetchRequest.getTimestamp());
                            return consentManager.getConsent(fetchRequest.getConsentId(), fetchRequest.getRequestId());
                        }).subscriberContext(ctx -> {
                            Optional<String> correlationId = Optional.ofNullable(MDC.get(CORRELATION_ID));
                            return correlationId.map(id -> ctx.put(CORRELATION_ID, id))
                                    .orElseGet(() -> ctx.put(CORRELATION_ID, UUID.randomUUID().toString()));
                        }).subscribe())
                        .then());
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(value = PATH_HIP_CONSENT_ON_NOTIFY)
    public Mono<Void> hipOnNotify(@RequestBody HIPConsentNotificationAcknowledgment acknowledgment) {
        return Mono.just(acknowledgment)
                .filterWhen(req ->
                        validator.validate(acknowledgment.getRequestId().toString(), acknowledgment.getTimestamp()))
                .switchIfEmpty(Mono.error(ClientError.tooManyRequests()))
                .flatMap(validatedRequest ->
                        hipConsentArtefactStatus.exists(acknowledgment.getAcknowledgement().getConsentId())
                                .switchIfEmpty(Mono.just(false)))
                .flatMap(exists -> exists ? hipConsentArtefactStatus.put(acknowledgment.getAcknowledgement().getConsentId(), NOTIFIED.toString()) :
                        consentManager.updateConsentNotification(acknowledgment)
                                .then(validator.put(acknowledgment.getRequestId().toString(), acknowledgment.getTimestamp())));
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(value = PATH_HIU_CONSENT_ON_NOTIFY)
    public Mono<Void> hiuOnNotify(@RequestBody HIUConsentNotificationAcknowledgment acknowledgment) {
        return Mono.just(acknowledgment)
                .filterWhen(req ->
                        validator.validate(acknowledgment.getRequestId().toString(), acknowledgment.getTimestamp()))
                .switchIfEmpty(Mono.error(ClientError.tooManyRequests()))
                .doOnSuccess(discard -> logger.info("Successfully notified from hiu for consent with request id {}", acknowledgment.getRequestId()))
                .then();
    }

    @GetMapping(value = Constants.APP_PATH_INTERNAL_GET_CONSENT_ARTEFACT_STATUS)
    public Mono<ConsentArtefactsStatusResponse> consentArtefactsStatus(@PathVariable String consentId) {
        return hipConsentArtefactStatus.exists(consentId)
                .flatMap(exists -> exists ? hipConsentArtefactStatus.get(consentId)
                        .flatMap(status -> Mono.just(ConsentArtefactsStatusResponse.builder().status(status).build()))
                        : Mono.error(ClientError.invalidRequester()));
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(value = CONSENT_REQUESTS_STATUS)
    public Mono<Void> status(@Valid @RequestBody ConsentRequestStatus consentRequestStatus) {
        return Mono.just(consentRequestStatus)
                .filterWhen(req -> validator
                        .validate(consentRequestStatus.getRequestId().toString(), consentRequestStatus.getTimestamp()))
                .switchIfEmpty(Mono.error(ClientError.tooManyRequests()))
                .doOnSuccess(validatedRequest -> Mono.defer(() -> {
                    validator.put(consentRequestStatus.getRequestId().toString(), consentRequestStatus.getTimestamp());
                    return consentManager.getStatus(consentRequestStatus);
                }).subscriberContext(ctx -> {
                    Optional<String> correlationId = Optional.ofNullable(MDC.get(CORRELATION_ID));
                    return correlationId.map(id -> ctx.put(CORRELATION_ID, id))
                            .orElseGet(() -> ctx.put(CORRELATION_ID, UUID.randomUUID().toString()));
                }).subscribe())
                .then();
    }

    private int getPageSize(int limit) {
        if (limit < 0) {
            return serviceProperties.getDefaultPageSize();
        }
        return Math.min(limit, serviceProperties.getMaxPageSize());
    }
}
