package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.common.Authenticator;
import in.projecteka.consentmanager.common.Caller;
import in.projecteka.consentmanager.consent.model.ConsentRequestValidator;
import in.projecteka.consentmanager.consent.model.request.ConsentApprovalRequest;
import in.projecteka.consentmanager.consent.model.request.ConsentRequest;
import in.projecteka.consentmanager.consent.model.response.ConsentApprovalResponse;
import in.projecteka.consentmanager.consent.model.response.ConsentRequestsRepresentation;
import in.projecteka.consentmanager.consent.model.response.RequestCreatedRepresentation;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@RestController
@AllArgsConstructor
public class ConsentRequestController {

    private final ConsentManager consentManager;
    private final ConsentServiceProperties serviceProperties;
    private final Authenticator authenticator;

    @InitBinder("consentRequest")
    protected void initBinder(WebDataBinder binder) {
        binder.addValidators(new ConsentRequestValidator());
    }

    @PostMapping(value = "/consent-requests")
    public Mono<RequestCreatedRepresentation> requestConsent(
            @RequestHeader(value = "Authorization") String token,
            @RequestBody @Valid @ModelAttribute("consentRequest") ConsentRequest request) {
        return consentManager.askForConsent(request.getConsent())
                .map(ConsentRequestController::buildResponse)
                .subscriberContext(context -> context.put(AUTHORIZATION, token));
    }

    @GetMapping(value = "/consent-requests")
    public Mono<ConsentRequestsRepresentation> allConsents(
            @RequestHeader(value = "Authorization") String token,
            @RequestParam(defaultValue = "-1") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        int pageSize = getPageSize(limit);
        return authenticator.userFrom(token)
                .flatMap(caller -> consentManager.findRequestsForPatient(caller.getUserName(), pageSize, offset))
                .map(results -> ConsentRequestsRepresentation.builder()
                        .size(results.size())
                        .requests(results)
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

    @PostMapping(value = "/consent-requests/{request-id}/approve")
    public Mono<ConsentApprovalResponse> approveConsent(
            @PathVariable(value = "request-id") String requestId,
            @RequestHeader(value = "Authorization") String token,
            @Valid @RequestBody ConsentApprovalRequest consentApprovalRequest) {
        return authenticator.userFrom(token)
                .map(Caller::getUserName)
                .flatMap(patient ->
                        consentManager.approveConsent(patient, requestId, consentApprovalRequest.getConsents())
                                .subscriberContext(context -> context.put(AUTHORIZATION, token)));
    }
}
