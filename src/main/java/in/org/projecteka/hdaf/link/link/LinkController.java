package in.org.projecteka.hdaf.link.link;

import in.org.projecteka.hdaf.link.link.model.PatientLinkReferenceRequest;
import in.org.projecteka.hdaf.link.link.model.PatientLinkReferenceResponse;
import in.org.projecteka.hdaf.link.link.model.PatientLinkRequest;
import in.org.projecteka.hdaf.link.link.model.PatientLinkResponse;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@AllArgsConstructor
public class LinkController {

    private Link link;

    @PostMapping("/patients/link")
    public Flux<PatientLinkReferenceResponse> linkCareContexts(@RequestHeader(value="Authorization") String authorization, @RequestBody PatientLinkReferenceRequest patientLinkReferenceRequest) {
        String patientId = TokenUtils.readUserId(authorization);
        return link.patientWith(patientId, patientLinkReferenceRequest);
    }

    @PostMapping("/patients/link/{linkRefNumber}")
    public Flux<PatientLinkResponse> verifyToken(@RequestHeader(value = "Authorization") String authorization, @PathVariable("linkRefNumber") String linkRefNumber, @RequestBody PatientLinkRequest patientLinkRequest) {
        String patientId = TokenUtils.readUserId(authorization);
        return link.verifyToken(patientId, linkRefNumber, patientLinkRequest);
    }
}
