package in.projecteka.consentmanager.userauth;

import in.projecteka.consentmanager.userauth.model.AuthInitRequest;
import reactor.core.publisher.Mono;

public interface HIPLinkInitAction {
    Mono<Void> execute(AuthInitRequest request);
}
