package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.consent.model.CertResponse;
import in.projecteka.consentmanager.consent.model.ConsentRequestValidator;
import in.projecteka.consentmanager.consent.model.request.ConsentApprovalRequest;
import in.projecteka.consentmanager.consent.model.request.ConsentRequest;
import in.projecteka.consentmanager.consent.model.response.ConsentApprovalResponse;
import in.projecteka.consentmanager.consent.model.response.ConsentRequestsRepresentation;
import in.projecteka.consentmanager.consent.model.response.RequestCreatedRepresentation;
import in.projecteka.library.clients.model.ClientError;
import in.projecteka.library.common.Caller;
import in.projecteka.library.common.RequestValidator;
import in.projecteka.library.common.cache.CacheAdapter;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

import static in.projecteka.consentmanager.consent.Constants.PATH_CONSENT_REQUESTS_INIT;

@RestController
@AllArgsConstructor
public class ConsentRequestController {
    private final ConsentManager consentManager;
    private final ConsentServiceProperties serviceProperties;
    private final CacheAdapter<String, String> usedTokens;
    private final RequestValidator validator;

    private static final Logger logger = LoggerFactory.getLogger(ConsentRequestController.class);

    @InitBinder("consentRequest")
    protected void initBinder(WebDataBinder binder) {
        binder.addValidators(new ConsentRequestValidator());
    }

    @Deprecated
    @PostMapping(value = "/consent-requests")
    public Mono<RequestCreatedRepresentation> requestConsent(
            @RequestBody @Valid @ModelAttribute("consentRequest") ConsentRequest request) {
        return consentManager.askForConsent(request.getConsent(), request.getRequestId())
                .map(ConsentRequestController::buildResponse);
    }


    @PostMapping(value = PATH_CONSENT_REQUESTS_INIT)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Void> initConsentRequest(
            @RequestBody @Valid @ModelAttribute("consentRequest") ConsentRequest request) {
        return Mono.just(request)
                .filterWhen(req -> validator.validate(request.getRequestId().toString(), request.getTimestamp()))
                .switchIfEmpty(Mono.error(ClientError.tooManyRequests()))
                .flatMap(validatedRequest ->
                        validator.put(request.getRequestId().toString(), request.getTimestamp())
                                .then(consentManager.requestConsent(request.getConsent(), request.getRequestId())));
    }

    @GetMapping(value = Constants.APP_PATH_GET_CONSENT_REQUESTS)
    public Mono<ConsentRequestsRepresentation> allConsents(
            @RequestParam(defaultValue = "-1") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "ALL") String status) {
        int pageSize = getPageSize(limit);
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .flatMap(caller -> consentManager
                        .findRequestsForPatient(caller.getUsername(), pageSize, offset, status))
                .map(requests -> ConsentRequestsRepresentation.builder()
                        .size(requests.getTotal())
                        .requests(requests.getResult())
                        .limit(pageSize)
                        .offset(offset)
                        .build());
    }

    private int getPageSize(int limit) {
        if (limit < 0) {
            return serviceProperties.getDefaultPageSize();
        }
        return Math.min(limit, serviceProperties.getMaxPageSize());
    }

    private static RequestCreatedRepresentation buildResponse(String requestId) {
        return RequestCreatedRepresentation.builder().consentRequestId(requestId).build();
    }

    @PostMapping(value = Constants.APP_PATH_APPROVE_CONSENT_REQUEST)
    public Mono<ConsentApprovalResponse> approveConsent(
            @PathVariable(value = "request-id") String requestId,
            @Valid @RequestBody ConsentApprovalRequest consentApprovalRequest) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .flatMap(caller ->
                        consentManager
                                .approveConsent(caller.getUsername(), requestId, consentApprovalRequest.getConsents())
                                .doOnSuccess(discard -> {
                                    logger.debug("[approve] putting {} in used tokens", caller.getSessionId());
                                    usedTokens.put(caller.getSessionId(), "").subscribe();
                                }));
    }

    @PostMapping(value = Constants.APP_PATH_DENY_CONSENT)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deny(@PathVariable(value = "id") String id) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getUsername)
                .flatMap(username -> consentManager.deny(id, username));
    }

    @GetMapping(value = Constants.GET_CONSENT_CERT)
    public Mono<CertResponse> getCert(){
        return consentManager.getCert();
    }
}
