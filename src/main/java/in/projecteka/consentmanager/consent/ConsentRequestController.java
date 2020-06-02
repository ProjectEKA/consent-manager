package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.common.Caller;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import in.projecteka.consentmanager.consent.model.ConsentRequestValidator;
import in.projecteka.consentmanager.consent.model.request.ConsentApprovalRequest;
import in.projecteka.consentmanager.consent.model.request.ConsentRequest;
import in.projecteka.consentmanager.consent.model.response.ConsentApprovalResponse;
import in.projecteka.consentmanager.consent.model.response.ConsentRequestsRepresentation;
import in.projecteka.consentmanager.consent.model.response.RequestCreatedRepresentation;
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

@RestController
@AllArgsConstructor
public class ConsentRequestController {
	private final ConsentManager consentManager;
	private final ConsentServiceProperties serviceProperties;
	private final CacheAdapter<String, String> usedTokens;
	private static final Logger logger = LoggerFactory.getLogger(ConsentRequestController.class);

	@InitBinder("consentRequest")
	protected void initBinder(WebDataBinder binder) {
		binder.addValidators(new ConsentRequestValidator());
	}

	@PostMapping(value = "/consent-requests")
	public Mono<RequestCreatedRepresentation> requestConsent(
			@RequestBody @Valid @ModelAttribute("consentRequest") ConsentRequest request) {
		return consentManager.askForConsent(request.getConsent(), request.getRequestId())
				.map(ConsentRequestController::buildResponse);
	}

	@GetMapping(value = "/consent-requests")
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

	@PostMapping(value = "/consent-requests/{request-id}/approve")
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

	@PostMapping(value = "/consent-requests/{id}/deny")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public Mono<Void> deny(@PathVariable(value = "id") String id) {
		return ReactiveSecurityContextHolder.getContext()
				.map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
				.map(Caller::getUsername)
				.flatMap(username -> consentManager.deny(id, username));
	}
}
