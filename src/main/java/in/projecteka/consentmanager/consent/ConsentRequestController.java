package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.consent.model.request.ConsentRequest;
import in.projecteka.consentmanager.consent.model.response.ConsentRequestResponse;
import in.projecteka.consentmanager.consent.repository.ConsentRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.UUID;

@RestController
@Validated
public class ConsentRequestController {

    @Autowired
    private ConsentRequestRepository requestRepository;

    @PostMapping(value = "/consent-requests")
    public Mono<ConsentRequestResponse> askForConsent(
            @RequestHeader(value = "Authorization") String authorization,
            @Valid @RequestBody Mono<ConsentRequest> request) {

        return request.flatMap(hiuRequest -> {
            final String requestId = UUID.randomUUID().toString();
            return requestRepository.insert(hiuRequest.getConsent(), requestId)
                    .then(Mono.just(ConsentRequestResponse.builder().requestId(requestId).build()));
        });
    }


}
