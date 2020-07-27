package in.projecteka.consentmanager.hipLinkInitialization;

import in.projecteka.consentmanager.hipLinkInitialization.model.request.AuthInitRequest;
import reactor.core.publisher.Mono;

public interface HIPLinkInitializer {
    Mono<Void> authInit(AuthInitRequest request);
}
