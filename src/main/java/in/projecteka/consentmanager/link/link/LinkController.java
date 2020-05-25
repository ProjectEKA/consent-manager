package in.projecteka.consentmanager.link.link;

import in.projecteka.consentmanager.clients.model.PatientLinkReferenceResponse;
import in.projecteka.consentmanager.clients.model.PatientLinkReferenceResult;
import in.projecteka.consentmanager.clients.model.PatientLinkRequest;
import in.projecteka.consentmanager.clients.model.PatientLinkResponse;
import in.projecteka.consentmanager.common.Caller;
import in.projecteka.consentmanager.link.link.model.LinkConfirmationResult;
import in.projecteka.consentmanager.link.link.model.PatientLinkReferenceRequest;
import in.projecteka.consentmanager.link.link.model.PatientLinksResponse;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

@RestController
@AllArgsConstructor
public class LinkController {

    private final Link link;

    @PostMapping("/patients/link")
    public Mono<PatientLinkReferenceResponse> linkCareContexts(
            @RequestBody PatientLinkReferenceRequest patientLinkReferenceRequest) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .flatMap(caller -> link.patientWith(caller.getUsername(), patientLinkReferenceRequest));
    }

    @Deprecated
    @PostMapping("/patients/link/{linkRefNumber}")
    public Mono<PatientLinkResponse> verifyToken(@PathVariable("linkRefNumber") String linkRefNumber,
                                                 @RequestBody PatientLinkRequest patientLinkRequest) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .flatMap(caller -> link.verifyToken(linkRefNumber, patientLinkRequest, caller.getUsername()));
    }

    @GetMapping("/patients/links")
    public Mono<PatientLinksResponse> getLinkedCareContexts() {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getUsername)
                .flatMap(link::getLinkedCareContexts);
    }

    @GetMapping("internal/patients/{username}/links")
    public Mono<PatientLinksResponse> getLinkedCareContextInternal(@PathVariable String username) {
        return link.getLinkedCareContexts(username);
    }

    @PostMapping("/v1/links/link/on-init")
    public Mono<Void> onLinkCareContexts(@RequestBody PatientLinkReferenceResult patientLinkReferenceResult) {
        return link.onLinkCareContexts(patientLinkReferenceResult);
    }

    /**
     * This API is intended for a CM App.
     * e.g. a mobile app or from other channels
     * @param linkRefNumber
     * @param patientLinkRequest
     * @return
     */
    @PostMapping("/v1/links/link/confirm")
    public Mono<PatientLinkResponse> confirmLink(@RequestBody PatientLinkRequest patientLinkRequest) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .flatMap(caller -> link.verifyLinkToken(caller.getUsername(), patientLinkRequest));
    }

    /**
     * HIP->Gateway Callback API for /links/link/confirm
     * @param confirmationResult
     * @return
     */
    @PostMapping("/v1/links/link/on-confirm")
    public Mono<Void> onConfirmLink(@RequestBody @Valid LinkConfirmationResult confirmationResult) {
        return link.onConfirmLink(confirmationResult);
    }
}
