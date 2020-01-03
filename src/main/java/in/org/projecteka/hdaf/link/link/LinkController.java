package in.org.projecteka.hdaf.link.link;

import in.org.projecteka.hdaf.link.link.model.PatientLinkReferenceRequest;
import in.org.projecteka.hdaf.link.link.model.PatientLinkReferenceResponse;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
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

}
