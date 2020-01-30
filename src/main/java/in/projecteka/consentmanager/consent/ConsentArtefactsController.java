package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.common.TokenUtils;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactRepresentation;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@AllArgsConstructor
public class ConsentArtefactsController {

    private ConsentManager consentManager;

    @GetMapping(value = "/consent-artefacts/{consentId}")
    public Mono<ConsentArtefactRepresentation> getConsentArtefact(
            @RequestHeader(value = "Authorization") String authorization,
            @PathVariable(value = "consentId") String consentId) {
        String hiuId = TokenUtils.readUserId(authorization);
        return consentManager.getConsent(consentId, hiuId);
    }
}
