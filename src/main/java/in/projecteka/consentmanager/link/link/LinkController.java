package in.projecteka.consentmanager.link.link;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.model.PatientLinkReferenceResponse;
import in.projecteka.consentmanager.clients.model.PatientLinkReferenceResult;
import in.projecteka.consentmanager.clients.model.PatientLinkRequest;
import in.projecteka.consentmanager.clients.model.PatientLinkResponse;
import in.projecteka.consentmanager.common.Caller;
import in.projecteka.consentmanager.common.RequestValidator;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import in.projecteka.consentmanager.link.Constants;
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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static in.projecteka.consentmanager.link.Constants.PATH_LINK_ON_CONFIRM;
import static in.projecteka.consentmanager.link.Constants.PATH_LINK_ON_INIT;


@RestController
@AllArgsConstructor
public class LinkController {
    private final RequestValidator validator;
    private final CacheAdapter<String, String> cacheForReplayAttack;
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
    public Mono<Void> onLinkCareContexts(@RequestBody PatientLinkReferenceResult patientLinkReferenceResult) {
        return Mono.just(patientLinkReferenceResult)
                .filterWhen(res -> validator.validate(patientLinkReferenceResult.getRequestId().toString(),
                        convertTimestampToLocalDateTimeUTC(patientLinkReferenceResult.getTimestamp()).toString()))
                .switchIfEmpty(Mono.error(ClientError.tooManyRequests()))
                .flatMap(res -> cacheForReplayAttack.put(
                        patientLinkReferenceResult.getRequestId().toString(),
                        patientLinkReferenceResult.getTimestamp()
                )
                        .then(link.onLinkCareContexts(patientLinkReferenceResult)));
    }

    /**
     * This API is intended for a CM App.
     * e.g. a mobile app or from other channels
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
     * @param confirmationResult
     * @return
     */
    @PostMapping(PATH_LINK_ON_CONFIRM)
    public Mono<Void> onConfirmLink(@RequestBody @Valid LinkConfirmationResult confirmationResult) {
        return Mono.just(confirmationResult)
                .filterWhen(req -> validator.validate(confirmationResult.getRequestId().toString()
                        ,convertTimestampToLocalDateTimeUTC(confirmationResult.getTimestamp()).toString()))
                .switchIfEmpty(Mono.error(ClientError.tooManyRequests()))
                .flatMap(validatedRequest -> link.onConfirmLink(confirmationResult)
                        .then(cacheForReplayAttack.put(confirmationResult.getRequestId().toString(),
                                                       confirmationResult.getTimestamp())));
    }

    @PostMapping(Constants.APP_PATH_LINK_INIT)
    public Mono<PatientLinkReferenceResponse> linkPatientCareContexts(
            @RequestBody PatientLinkReferenceRequest patientLinkReferenceRequest) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .flatMap(caller -> link.patientCareContexts(caller.getUsername(), patientLinkReferenceRequest));
    }

    private LocalDateTime convertTimestampToLocalDateTimeUTC(String timestamp) {
        return LocalDateTime.ofInstant(
                Instant.parse(timestamp), ZoneOffset.UTC);
    }

}
