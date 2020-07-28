package in.projecteka.consentmanager.link.hip_link;

import in.projecteka.consentmanager.link.hip_link.model.request.AuthInitRequest;
import reactor.core.publisher.Mono;

public class UserAuthInitAction implements HIPLinkInitAction {

    @Override
    public Mono<Void> execute(AuthInitRequest request) {
        return Mono.empty();
    }
}
