package in.projecteka.consentmanager.link.link;

import in.projecteka.consentmanager.clients.model.PatientLinkReferenceResponse;
import in.projecteka.consentmanager.clients.model.PatientLinkRequest;
import in.projecteka.consentmanager.clients.model.PatientLinkResponse;
import in.projecteka.consentmanager.common.Authenticator;
import in.projecteka.consentmanager.common.Caller;
import in.projecteka.consentmanager.link.link.model.PatientLinkReferenceRequest;
import in.projecteka.consentmanager.link.link.model.PatientLinksResponse;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@AllArgsConstructor
public class LinkController {

    private Link link;
    private Authenticator authenticator;

    @PostMapping("/patients/link")
    public Mono<PatientLinkReferenceResponse> linkCareContexts(
            @RequestHeader(value = "Authorization") String token,
            @RequestBody PatientLinkReferenceRequest patientLinkReferenceRequest) {
        return authenticator.userFrom(token)
                .flatMap(caller -> link.patientWith(caller.getUserName(), patientLinkReferenceRequest));
    }

    @PostMapping("/patients/link/{linkRefNumber}")
    public Mono<PatientLinkResponse> verifyToken(@RequestHeader(value = "Authorization") String token,
                                                 @PathVariable("linkRefNumber") String linkRefNumber,
                                                 @RequestBody PatientLinkRequest patientLinkRequest) {
        return authenticator.userFrom(token)
                .flatMap(caller -> link.verifyToken(linkRefNumber, patientLinkRequest, caller.getUserName()));
    }

    @GetMapping("/patients/links")
    public Mono<PatientLinksResponse> getLinkedCareContexts(@RequestHeader(value = "Authorization") String token) {
        return authenticator.userFrom(token)
                .map(Caller::getUserName)
                .flatMap(patient -> link.getLinkedCareContexts(patient));
    }

    @GetMapping("internal/patients/{username}/links")
    public Mono<PatientLinksResponse> getLinkedCareContextInternal(@PathVariable String username) {
        return link.getLinkedCareContexts(username);
    }
}
