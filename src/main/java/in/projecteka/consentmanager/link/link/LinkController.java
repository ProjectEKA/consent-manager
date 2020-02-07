package in.projecteka.consentmanager.link.link;

import in.projecteka.consentmanager.common.TokenUtils;
import in.projecteka.consentmanager.link.link.model.PatientLinkReferenceRequest;
import in.projecteka.consentmanager.link.link.model.PatientLinkReferenceResponse;
import in.projecteka.consentmanager.link.link.model.PatientLinkRequest;
import in.projecteka.consentmanager.link.link.model.PatientLinkResponse;
import in.projecteka.consentmanager.link.link.model.PatientLinksResponse;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@AllArgsConstructor
public class LinkController {

    private Link link;

    @PostMapping("/patients/link")
    public Mono<PatientLinkReferenceResponse> linkCareContexts(@RequestHeader(value = "Authorization") String authorization, @RequestBody PatientLinkReferenceRequest patientLinkReferenceRequest) {
        String patientId = TokenUtils.getCallerId(authorization);
        return link.patientWith(patientId, patientLinkReferenceRequest);
    }

    @PostMapping("/patients/link/{linkRefNumber}")
    public Mono<PatientLinkResponse> verifyToken(@RequestHeader(value = "Authorization") String authorization,
                                                 @PathVariable("linkRefNumber") String linkRefNumber,
                                                 @RequestBody PatientLinkRequest patientLinkRequest) {
        String patientId = TokenUtils.getCallerId(authorization);
        return link.verifyToken(linkRefNumber, patientLinkRequest, patientId);
    }

    @GetMapping("/patients/links")
    public Mono<PatientLinksResponse> getLinkedCareContexts(@RequestHeader(value = "Authorization") String authorization) {
        String patientId = TokenUtils.getCallerId(authorization);
        return link.getLinkedCareContexts(patientId);
    }
}
