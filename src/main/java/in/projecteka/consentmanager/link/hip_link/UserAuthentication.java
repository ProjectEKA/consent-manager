package in.projecteka.consentmanager.link.hip_link;

import in.projecteka.consentmanager.link.hip_link.model.request.AuthInitRequest;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class UserAuthentication {
    HIPLinkInitAction initAction;
    private final Logger logger = LoggerFactory.getLogger(UserAuthentication.class);

    public Mono<Void> authInit(AuthInitRequest request) {
        logger.info("User auth initialization");
        initAction.execute(request);
        return Mono.empty();
    }
}
