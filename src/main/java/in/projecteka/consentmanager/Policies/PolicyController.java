package in.projecteka.consentmanager.Policies;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@AllArgsConstructor
public class PolicyController {
    private final PolicyService policyService;

    @PostMapping("/internal/policy/consent-request/{consentRequestId}")
    public Mono<Void> internalUserWith(@PathVariable String consentRequestId) {
        policyService.checkPolicyFor(consentRequestId);
        return Mono.empty();
    }
}
