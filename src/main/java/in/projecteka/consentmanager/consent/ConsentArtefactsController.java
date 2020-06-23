package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.common.Caller;
import in.projecteka.consentmanager.common.ServiceCaller;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import in.projecteka.consentmanager.consent.model.FetchRequest;
import in.projecteka.consentmanager.consent.model.RevokeRequest;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactLightRepresentation;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactRepresentation;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactResponse;
import in.projecteka.consentmanager.consent.model.response.HIPCosentNotificationAcknowledgment;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import static in.projecteka.consentmanager.common.Constants.V_1_CONSENTS_FETCH;
import static in.projecteka.consentmanager.common.Constants.V_1_HIP_CONSENT_ON_NOTIFY;

@RestController
@AllArgsConstructor
public class ConsentArtefactsController {

    private static final Logger logger = LoggerFactory.getLogger(ConsentArtefactsController.class);
    private final ConsentManager consentManager;
    private final CacheAdapter<String, String> usedTokens;
    private final ConsentServiceProperties serviceProperties;

    @GetMapping(value = "/consents/{consentId}")
    public Mono<ConsentArtefactRepresentation> getConsentArtefact(@PathVariable(value = "consentId") String consentId) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (ServiceCaller) securityContext.getAuthentication().getPrincipal())
                .flatMap(requester -> consentManager.getConsent(consentId, requester.getClientId()));
    }

    @GetMapping(value = "/internal/consents/{consentId}")
    public Mono<ConsentArtefactLightRepresentation> getConsent(@PathVariable String consentId) {
        return consentManager.getConsentArtefactLight(consentId);
    }

    @GetMapping(value = "/consent-requests/{request-id}/consent-artefacts")
    public Flux<ConsentArtefactRepresentation> getConsents(@PathVariable(value = "request-id") String requestId) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getUsername)
                .flatMapMany(patient -> consentManager.getConsents(requestId, patient));
    }

    @GetMapping(value = "/consent-artefacts")
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

    @PostMapping(value = "/consents/revoke")
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
    @PostMapping(value = V_1_CONSENTS_FETCH)
    public Mono<Void> fetchConsent(@RequestBody FetchRequest fetchRequest) {
      return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (ServiceCaller) securityContext.getAuthentication().getPrincipal())
                .doOnSuccess(requester -> {
                    Mono.defer(() -> consentManager.getConsent(fetchRequest.getConsentId(), fetchRequest.getRequestId())).subscribe();
                })
                .then();
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(value = V_1_HIP_CONSENT_ON_NOTIFY)
    public Mono<Void> hipOnNotify(@RequestBody HIPCosentNotificationAcknowledgment acknowledgment) {
        return consentManager.updateConsentNotification(acknowledgment);
    }

    private int getPageSize(int limit) {
        if (limit < 0) {
            return serviceProperties.getDefaultPageSize();
        }
        return Math.min(limit, serviceProperties.getMaxPageSize());
    }
}
