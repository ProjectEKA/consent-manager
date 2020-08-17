package in.projecteka.consentmanager.link.hiplink;

import in.projecteka.consentmanager.link.hiplink.model.request.AuthInitRequest;
import reactor.core.publisher.Mono;

public interface HIPLinkInitAction {
    Mono<Void> execute(AuthInitRequest request);
}
