package in.projecteka.consentmanager.userauth;

import in.projecteka.consentmanager.userauth.model.AuthInitRequest;
import reactor.core.publisher.Mono;

public class UserAuthInitAction implements HIPLinkInitAction {

    @Override
    public Mono<Void> execute(AuthInitRequest request) {
        //TODO
        return Mono.empty();
    }
}
