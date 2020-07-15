package in.projecteka.consentmanager.Policies;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class PolicyService {
    public Mono<Void> checkPolicyFor(String consentRequestId){
        System.out.println(consentRequestId);
        return Mono.empty();
    }
}
