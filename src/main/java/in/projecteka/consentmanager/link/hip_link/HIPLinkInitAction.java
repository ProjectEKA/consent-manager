package in.projecteka.consentmanager.link.hip_link;

import in.projecteka.consentmanager.link.hip_link.model.request.AuthInitRequest;
import reactor.core.publisher.Mono;

public interface HIPLinkInitAction {
    Mono<Void> execute(AuthInitRequest request);
}
