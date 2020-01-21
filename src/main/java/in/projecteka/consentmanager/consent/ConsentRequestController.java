package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.consent.model.ConsentRequestValidator;
import in.projecteka.consentmanager.consent.model.request.ConsentRequest;
import in.projecteka.consentmanager.consent.model.response.ConsentRequestResponse;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.*;

@RestController
@AllArgsConstructor
public class ConsentRequestController {

    private ConsentManager consentManager;

    @InitBinder
    protected void initBinder(WebDataBinder binder) {
        binder.addValidators(new ConsentRequestValidator());
    }

    @PostMapping(value = "/consent-requests")
    public Mono<ConsentRequestResponse> requestConsent(
            @RequestHeader(value = "Authorization") String authorization,
            @RequestBody @Valid ConsentRequest request) {
        return consentManager.askForConsent(authorization, request.getConsent()).map(this::buildResponse);
    }

    private ConsentRequestResponse buildResponse(String requestId) {
        return new ConsentRequestResponse(requestId);
    }
}
