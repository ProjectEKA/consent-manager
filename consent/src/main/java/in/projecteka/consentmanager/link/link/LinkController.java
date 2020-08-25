package in.projecteka.consentmanager.link.link;

import in.projecteka.consentmanager.clients.model.PatientLinkReferenceResponse;
import in.projecteka.consentmanager.clients.model.PatientLinkReferenceResult;
import in.projecteka.consentmanager.clients.model.PatientLinkRequest;
import in.projecteka.consentmanager.clients.model.PatientLinkResponse;
import in.projecteka.consentmanager.link.Constants;
import in.projecteka.consentmanager.link.link.model.LinkConfirmationResult;
import in.projecteka.consentmanager.link.link.model.LinkRequest;
import in.projecteka.consentmanager.link.link.model.PatientLinkReferenceRequest;
import in.projecteka.consentmanager.link.link.model.PatientLinksResponse;
import in.projecteka.library.clients.model.ClientError;
import in.projecteka.library.common.Caller;
import in.projecteka.library.common.RequestValidator;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

import static in.projecteka.consentmanager.link.Constants.APP_PATH_LINK_INIT;
import static in.projecteka.consentmanager.link.Constants.PATH_HIP_ADD_CONTEXTS;
import static in.projecteka.consentmanager.link.Constants.PATH_LINK_ON_CONFIRM;
import static in.projecteka.consentmanager.link.Constants.PATH_LINK_ON_INIT;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static reactor.core.publisher.Mono.just;

@RestController
@AllArgsConstructor
public class LinkController {
    private final RequestValidator validator;
    private final Link link;

    @GetMapping(Constants.APP_PATH_GET_PATIENTS_LINKS)
    public Mono<PatientLinksResponse> getLinkedCareContexts() {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getUsername)
                .flatMap(link::getLinkedCareContexts);
    }

    @GetMapping(Constants.APP_PATH_INTERNAL_GET_LINKED_CARE_CONTEXTS)
    public Mono<PatientLinksResponse> getLinkedCareContextInternal(@PathVariable String username) {
        return link.getLinkedCareContexts(username);
    }

    @PostMapping(PATH_LINK_ON_INIT)
    public Mono<Void> onLinkCareContexts(@RequestBody PatientLinkReferenceResult result) {
        return just(result)
                .filterWhen(res -> validator.validate(result.getRequestId().toString(), result.getTimestamp()))
                .switchIfEmpty(Mono.error(ClientError.tooManyRequests()))
                .flatMap(res -> validator.put(result.getRequestId().toString(), result.getTimestamp())
                        .then(link.onLinkCareContexts(result)));
    }

    /**
     * This API is intended for a CM App.
     * e.g. a mobile app or from other channels
     *
     * @param linkRefNumber
     * @param patientLinkRequest
     * @return
     */
    @PostMapping(in.projecteka.consentmanager.link.Constants.APP_PATH_CONFIRM_LINK_REF_NUMBER)
    public Mono<PatientLinkResponse> confirmLink(
            @PathVariable("linkRefNumber") String linkRefNumber,
            @RequestBody PatientLinkRequest patientLinkRequest) {
        patientLinkRequest.setLinkRefNumber(linkRefNumber);
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .flatMap(caller -> link.verifyLinkToken(caller.getUsername(), patientLinkRequest));
    }

    /**
     * HIP->Gateway Callback API for /links/link/confirm
     *
     * @param result
     * @return
     */
    @PostMapping(PATH_LINK_ON_CONFIRM)
    public Mono<Void> onConfirmLink(@RequestBody @Valid LinkConfirmationResult result) {
        return just(result)
                .filterWhen(req -> validator.validate(result.getRequestId().toString(), result.getTimestamp()))
                .switchIfEmpty(Mono.error(ClientError.tooManyRequests()))
                .flatMap(discard -> link.onConfirmLink(result)
                        .then(validator.put(result.getRequestId().toString(), result.getTimestamp())));
    }

    @PostMapping(APP_PATH_LINK_INIT)
    public Mono<PatientLinkReferenceResponse> linkPatientCareContexts(
            @RequestBody PatientLinkReferenceRequest patientLinkReferenceRequest) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .flatMap(caller -> link.patientCareContexts(caller.getUsername(), patientLinkReferenceRequest));
    }

    @PostMapping(PATH_HIP_ADD_CONTEXTS)
    @ResponseStatus(ACCEPTED)
    public Mono<Void> linkCareContexts(@RequestBody LinkRequest linkRequest) {
        return just(linkRequest)
                .filterWhen(req ->
                        validator.validate(linkRequest.getRequestId().toString(), linkRequest.getTimestamp()))
                .switchIfEmpty(Mono.error(ClientError.tooManyRequests()))
                .doOnSuccess(requester -> Mono.defer(() -> {
                    validator.put(
                            linkRequest.getRequestId().toString(), linkRequest.getTimestamp());
                    return link.addCareContexts(linkRequest);
                }).subscribe())
                .then();
    }
}
