package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.common.TokenUtils;
import in.projecteka.consentmanager.consent.model.ConsentRequestValidator;
import in.projecteka.consentmanager.consent.model.request.ConsentRequest;
import in.projecteka.consentmanager.consent.model.response.ConsentRequestDetail;
import in.projecteka.consentmanager.consent.model.response.ConsentRequestsRepresentation;
import in.projecteka.consentmanager.consent.model.response.RequestCreatedRepresentation;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.*;
import java.util.List;

@RestController
@AllArgsConstructor
public class ConsentRequestController {

    private ConsentManager hdcm;
    private ConsentServiceProperties serviceProperties;

    @InitBinder
    protected void initBinder(WebDataBinder binder) {
        binder.addValidators(new ConsentRequestValidator());
    }

    @PostMapping(value = "/consent-requests")
    public Mono<RequestCreatedRepresentation> requestConsent(
            @RequestHeader(value = "Authorization", required = true) String authorization,
            @RequestBody @Valid Mono<ConsentRequest> request) {

        return request.flatMap(r -> hdcm.askForConsent(authorization, r.getConsent())).map(this::buildResponse);
    }


    @GetMapping(value = "/consent-requests")
    public Mono<ConsentRequestsRepresentation> allConsents(
            @RequestHeader(value = "Authorization") String authorization,
            @RequestParam(defaultValue = "-1") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        String patientId = TokenUtils.readUserId(authorization);
        int pageSize = getPageSize(limit);
        return hdcm.findRequestsForPatient(patientId, pageSize, offset)
                .flatMap(results -> Mono.just(ConsentRequestsRepresentation.builder()
                        .size(results.size())
                        .requests(results)
                        .limit(pageSize)
                        .offset(offset)
                        .build()));
    }

    private int getPageSize(int limit) {
        if (limit < 0) {
            return serviceProperties.getDefaultPageSize();
        }
        if (limit > serviceProperties.getMaxPageSize()) {
            return serviceProperties.getMaxPageSize();
        }
        return limit;
    }

    private RequestCreatedRepresentation buildResponse(String requestId) {
        return RequestCreatedRepresentation.builder().consentRequestId(requestId).build();
    }



}
