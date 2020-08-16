package in.projecteka.consentmanager.link.hiplink;

import in.projecteka.consentmanager.link.hiplink.model.request.AuthInitRequest;
import reactor.core.publisher.Mono;

public class UserAuthInitAction implements HIPLinkInitAction {

    @Override
    public Mono<Void> execute(AuthInitRequest request) {
        return Mono.empty();
    }
}
