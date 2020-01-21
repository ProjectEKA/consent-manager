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

    private ConsentManager hdcm;

    @InitBinder
    protected void initBinder(WebDataBinder binder) {
        binder.addValidators(new ConsentRequestValidator());
    }

    @PostMapping(value = "/consent-requests")
    public Mono<ConsentRequestResponse> requestConsent(
            @RequestHeader(value = "Authorization", required = true) String authorization,
            @RequestBody @Valid Mono<ConsentRequest> request) {

        return request.flatMap(r -> hdcm.askForConsent(authorization, r.getConsent())).flatMap(this::buildResponse);
    }

    private Mono<ConsentRequestResponse> buildResponse(String requestId) {
        return Mono.just(ConsentRequestResponse.builder().consentRequestId(requestId).build());
    }

}
