package in.projecteka.consentmanager.hipLinkInitialization;

import in.projecteka.consentmanager.hipLinkInitialization.model.request.AuthInitRequest;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class UserAuthService implements HIPLinkInitializer {
    private final Logger logger = LoggerFactory.getLogger(UserAuthService.class);

    @Override
    public Mono<Void> authInit(AuthInitRequest request) {
        logger.info("User auth initialization");
        return null;
    }
}
