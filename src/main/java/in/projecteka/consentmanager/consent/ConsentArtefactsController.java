package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.common.Caller;
import in.projecteka.consentmanager.consent.model.RevokeRequest;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactLightRepresentation;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactRepresentation;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@AllArgsConstructor
public class ConsentArtefactsController {

    private final ConsentManager consentManager;

    @GetMapping(value = "/consents/{consentId}")
    public Mono<ConsentArtefactRepresentation> getConsentArtefact(@PathVariable(value = "consentId") String consentId) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .flatMap(requester -> consentManager.getConsent(consentId, requester.getUserName()));
    }

    @GetMapping(value = "/internal/consents/{consentId}")
    public Mono<ConsentArtefactLightRepresentation> getConsent(@PathVariable String consentId) {
        return consentManager.getConsentArtefactLight(consentId);
    }

    @GetMapping(value = "/consent-requests/{request-id}/consent-artefacts")
    public Flux<ConsentArtefactRepresentation> getConsents(@PathVariable(value = "request-id") String requestId) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getUserName)
                .flatMapMany(patient -> consentManager.getConsents(requestId, patient));
    }

    @PostMapping(value = "/consents/revoke")
    public Mono<Void> revokeConsent(@RequestHeader(value = "Authorization") String token,
                                    @RequestBody RevokeRequest revokeRequest) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getUserName)
                .flatMap(requesterId -> consentManager.revokeAndBroadCastConsent(revokeRequest, requesterId));
    }
}
